/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.security.realms;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.events.AuthorizationConfigurationChanged;
import org.sonatype.security.events.SecurityConfigurationChanged;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CRole;
import org.sonatype.security.realms.privileges.PrivilegeDescriptor;
import org.sonatype.security.realms.tools.ConfigurationManager;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link RolePermissionResolver}.
 */
@Singleton
@Typed(RolePermissionResolver.class)
@Named("default")
public class RolePermissionResolverImpl
    extends ComponentSupport
    implements RolePermissionResolver
{
  private final ConfigurationManager configuration;

  private final List<PrivilegeDescriptor> privilegeDescriptors;

  private final Map<String, Collection<Permission>> permissionsCache;

  @Inject
  public RolePermissionResolverImpl(final ConfigurationManager configuration,
                                    final List<PrivilegeDescriptor> privilegeDescriptors,
                                    final EventBus eventBus)
  {
    this.configuration = checkNotNull(configuration);
    this.privilegeDescriptors = checkNotNull(privilegeDescriptors);
    this.permissionsCache = new MapMaker().weakValues().makeMap();
    eventBus.register(this);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AuthorizationConfigurationChanged event) {
    // invalidate previous results
    permissionsCache.clear();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final SecurityConfigurationChanged event) {
    // invalidate previous results
    permissionsCache.clear();
  }

  public Collection<Permission> resolvePermissionsInRole(final String roleString) {
    checkNotNull(roleString);

    final Set<Permission> permissions = Sets.newLinkedHashSet();
    final LinkedList<String> rolesToProcess = Lists.newLinkedList();
    final Set<String> processedRoleIds = Sets.newLinkedHashSet();

    // initial role
    rolesToProcess.add(roleString);

    while (!rolesToProcess.isEmpty()) {
      final String roleId = rolesToProcess.removeFirst();
      if (processedRoleIds.add(roleId)) {
        try {
          final CRole role = configuration.readRole(roleId);

          // check memory-sensitive cache (after readRole to allow for the dirty check)
          final Collection<Permission> cachedPermissions = permissionsCache.get(roleId);
          if (cachedPermissions != null) {
            permissions.addAll(cachedPermissions);
            continue; // use cached results
          }

          // process the roles this role has recursively
          rolesToProcess.addAll(role.getRoles());

          // add the permissions this role has
          for (String privilegeId : role.getPrivileges()) {
            Permission permission = permission(privilegeId);
            if (permission != null) {
              permissions.add(permission);
            }
          }
        }
        catch (NoSuchRoleException e) {
          log.trace("Ignoring missing role: {}", roleId, e);
        }
      }
    }

    // cache result of (non-trivial) computation
    permissionsCache.put(roleString, permissions);

    return permissions;
  }

  @Nullable
  private PrivilegeDescriptor descriptor(final String privilegeType) {
    assert privilegeType != null;

    for (PrivilegeDescriptor descriptor : privilegeDescriptors) {
      if (privilegeType.equals(descriptor.getType())) {
        return descriptor;
      }
    }

    log.warn("Missing privilege-descriptor for type: {}", privilegeType);
    return null;
  }

  @Nullable
  private Permission permission(final String privilegeId) {
    assert privilegeId != null;

    // TODO: Consider cache here?
    try {
      CPrivilege privilege = configuration.readPrivilege(privilegeId);
      PrivilegeDescriptor descriptor = descriptor(privilege.getType());
      if (descriptor != null) {
        return descriptor.createPermission(privilege);
      }
    }
    catch (NoSuchPrivilegeException e) {
      log.trace("Ignoring missing privilege: {}", privilegeId, e);
    }

    return null;
  }
}
