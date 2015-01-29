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
package org.sonatype.security.realms.privileges;

import org.sonatype.security.authorization.WildcardPermission2;
import org.sonatype.security.model.CPrivilege;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

/**
 * Support for {@link PrivilegeDescriptor} implementations using {@link WildcardPermission}.
 *
 * @since 3.0
 */
public abstract class WildcardPrivilegeDescriptorSupport
  extends PrivilegeDescriptorSupport
{
  public WildcardPrivilegeDescriptorSupport(final String type) {
    super(type);
  }

  /**
   * Format permission string for given privilege.
   */
  protected abstract String formatPermission(final CPrivilege privilege);

  @Override
  public Permission createPermission(final CPrivilege privilege) {
    assert privilege != null;
    assert getType().equals(privilege.getType());
    return new WildcardPermission2(formatPermission(privilege));
  }
}
