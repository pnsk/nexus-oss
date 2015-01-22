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

package org.sonatype.nexus.repository.simple.internal;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.security.CRoleBuilder;
import org.sonatype.nexus.repository.security.MutableDynamicSecurityResource.Mutator;
import org.sonatype.nexus.repository.security.RepositoryInstancePrivilegeDescriptor;
import org.sonatype.security.model.SecurityModelConfiguration;

import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.security.BreadActions.ADD;
import static org.sonatype.nexus.repository.security.BreadActions.BROWSE;
import static org.sonatype.nexus.repository.security.BreadActions.DELETE;
import static org.sonatype.nexus.repository.security.BreadActions.EDIT;
import static org.sonatype.nexus.repository.security.BreadActions.READ;
import static org.sonatype.nexus.repository.security.RepositoryInstancePrivilegeDescriptor.id;
import static org.sonatype.nexus.repository.security.RepositoryInstancePrivilegeDescriptor.privilege;

/**
 * Simple security facet.
 *
 * @since 3.0
 */
@Named
@Facet.Exposed
public class SimpleSecurityFacet
    extends FacetSupport
{
  private final SimpleDynamicSecurityResource dynamicSecurityResource;

  @Inject
  public SimpleSecurityFacet(final SimpleDynamicSecurityResource dynamicSecurityResource) {
    this.dynamicSecurityResource = checkNotNull(dynamicSecurityResource);
  }

  @Override
  protected void doStart() throws Exception {
    final String repositoryName = getRepository().getName();

    dynamicSecurityResource.apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        // add repository-instance privileges
        model.addPrivilege(privilege(repositoryName, BROWSE));
        model.addPrivilege(privilege(repositoryName, READ));
        model.addPrivilege(privilege(repositoryName, EDIT));
        model.addPrivilege(privilege(repositoryName, ADD));
        model.addPrivilege(privilege(repositoryName, DELETE));

        // add repository-instance 'admin' role
        model.addRole(new CRoleBuilder()
            .id(String.format("%s-%s-admin", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName))
            .privilege(id(repositoryName, BROWSE))
            .privilege(id(repositoryName, READ))
            .privilege(id(repositoryName, EDIT))
            .privilege(id(repositoryName, ADD))
            .privilege(id(repositoryName, DELETE))
            .create());

        // add repository-instance 'readonly' role
        model.addRole(new CRoleBuilder()
            .id(String.format("%s-%s-readonly", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName))
            .privilege(id(repositoryName, BROWSE))
            .privilege(id(repositoryName, READ))
            .create());

        // add repository-instance 'deployer' role
        model.addRole(new CRoleBuilder()
            .id(String.format("%s-%s-deployer", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName))
            .privilege(id(repositoryName, BROWSE))
            .privilege(id(repositoryName, READ))
            .privilege(id(repositoryName, EDIT))
            .privilege(id(repositoryName, ADD))
            .create());
      }
    });
  }

  @Override
  protected void doStop() throws Exception {
    final String repositoryName = getRepository().getName();

    dynamicSecurityResource.apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        // remove repository-instance privileges
        model.removePrivilege(id(repositoryName, BROWSE));
        model.removePrivilege(id(repositoryName, READ));
        model.removePrivilege(id(repositoryName, EDIT));
        model.removePrivilege(id(repositoryName, ADD));
        model.removePrivilege(id(repositoryName, DELETE));

        // remove repository-instance roles
        model.removeRole(String.format("%s-%s-admin", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName));
        model.removeRole(String.format("%s-%s-readonly", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName));
        model.removeRole(String.format("%s-%s-deployer", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName));
      }
    });
  }

  /**
   * Check if subject has ANY of the given permissions.
   */
  public boolean anyPermitted(final Subject subject, final String... permissions) {
    boolean trace = log.isTraceEnabled();
    if (trace) {
      log.trace("Checking if subject '{}' has ANY of these permissions: {}",
          subject.getPrincipal(), Arrays.toString(permissions));
    }
    for (String permission : permissions) {
      if (subject.isPermitted(permission)) {
        if (trace) {
          log.trace("Subject '{}' has permission: {}", subject.getPrincipal(), permission);
        }
        return true;
      }
    }
    if (trace) {
      log.trace("Subject '{}' missing required permissions: {}",
          subject.getPrincipal(), Arrays.toString(permissions));
    }
    return false;
  }

  /**
   * Check if subject has ALL of the given permissions.
   */
  public boolean allPermitted(final Subject subject, final String... permissions) {
    boolean trace = log.isTraceEnabled();
    if (trace) {
      log.trace("Checking if subject '{}' has ALL of these permissions: {}",
          subject.getPrincipal(), Arrays.toString(permissions));
    }
    for (String permission : permissions) {
      if (!subject.isPermitted(permission)) {
        if (trace) {
          log.trace("Subject '{}' missing permission: {}", subject.getPrincipal(), permission);
        }
        return false;
      }
    }

    if (trace) {
      log.trace("Subject '{}' has required permissions: {}",
          subject.getPrincipal(), Arrays.toString(permissions));
    }
    return false;
  }
}
