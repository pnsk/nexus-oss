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

package org.sonatype.nexus.repository.security;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.security.model.CRoleBuilder;
import org.sonatype.security.model.SecurityModelConfiguration;
import org.sonatype.security.realms.tools.MutableDynamicSecurityResource;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.security.BreadActions.ADD;
import static org.sonatype.nexus.repository.security.BreadActions.BROWSE;
import static org.sonatype.nexus.repository.security.BreadActions.DELETE;
import static org.sonatype.nexus.repository.security.BreadActions.EDIT;
import static org.sonatype.nexus.repository.security.BreadActions.READ;

/**
 * Repository format security resource.
 *
 * @since 3.0
 */
public class RepositoryFormatSecurityResource
    extends MutableDynamicSecurityResource
{
  // NOTE: This is not ideal, but moving forward to allow sharing, refactor eventually

  private final Format format;

  public RepositoryFormatSecurityResource(final Format format) {
    this.format = checkNotNull(format);

    // apply initial configuration
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        initial(model);
      }
    });
  }

  /**
   * Initial (static) security configuration.
   */
  private void initial(final SecurityModelConfiguration model) {
    String formatName = format.getValue();

    // add repository-format privileges
    model.addPrivilege(RepositoryFormatPrivilegeDescriptor.privilege(formatName, BROWSE));
    model.addPrivilege(RepositoryFormatPrivilegeDescriptor.privilege(formatName, READ));
    model.addPrivilege(RepositoryFormatPrivilegeDescriptor.privilege(formatName, EDIT));
    model.addPrivilege(RepositoryFormatPrivilegeDescriptor.privilege(formatName, ADD));
    model.addPrivilege(RepositoryFormatPrivilegeDescriptor.privilege(formatName, DELETE));

    // add repository-format 'admin' role
    model.addRole(new CRoleBuilder()
        .id(String.format("%s-%s-admin", RepositoryFormatPrivilegeDescriptor.TYPE, formatName))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, BROWSE))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, READ))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, EDIT))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, ADD))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, DELETE))
        .create());

    // add repository-format 'readonly' role
    model.addRole(new CRoleBuilder()
        .id(String.format("%s-%s-readonly", RepositoryFormatPrivilegeDescriptor.TYPE, formatName))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, BROWSE))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, READ))
        .create());

    // add repository-format 'deployer' role
    model.addRole(new CRoleBuilder()
        .id(String.format("%s-%s-deployer", RepositoryFormatPrivilegeDescriptor.TYPE, formatName))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, BROWSE))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, READ))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, EDIT))
        .privilege(RepositoryFormatPrivilegeDescriptor.id(formatName, ADD))
        .create());
  }

  /**
   * Add security configuration for given repository.
   */
  public void add(final Repository repository) {
    checkNotNull(repository);
    final String repositoryName = repository.getName();

    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        // add repository-instance privileges
        model.addPrivilege(RepositoryInstancePrivilegeDescriptor.privilege(repositoryName, BROWSE));
        model.addPrivilege(RepositoryInstancePrivilegeDescriptor.privilege(repositoryName, READ));
        model.addPrivilege(RepositoryInstancePrivilegeDescriptor.privilege(repositoryName, EDIT));
        model.addPrivilege(RepositoryInstancePrivilegeDescriptor.privilege(repositoryName, ADD));
        model.addPrivilege(RepositoryInstancePrivilegeDescriptor.privilege(repositoryName, DELETE));

        // add repository-instance 'admin' role
        model.addRole(new CRoleBuilder()
            .id(String.format("%s-%s-admin", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, BROWSE))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, READ))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, EDIT))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, ADD))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, DELETE))
            .create());

        // add repository-instance 'readonly' role
        model.addRole(new CRoleBuilder()
            .id(String.format("%s-%s-readonly", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, BROWSE))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, READ))
            .create());

        // add repository-instance 'deployer' role
        model.addRole(new CRoleBuilder()
            .id(String.format("%s-%s-deployer", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, BROWSE))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, READ))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, EDIT))
            .privilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, ADD))
            .create());
      }
    });
  }

  /**
   * Remove security configuration for given repository.
   */
  public void remove(final Repository repository) {
    checkNotNull(repository);
    final String repositoryName = repository.getName();

    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        // remove repository-instance privileges
        model.removePrivilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, BROWSE));
        model.removePrivilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, READ));
        model.removePrivilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, EDIT));
        model.removePrivilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, ADD));
        model.removePrivilege(RepositoryInstancePrivilegeDescriptor.id(repositoryName, DELETE));

        // remove repository-instance roles
        model.removeRole(String.format("%s-%s-admin", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName));
        model.removeRole(String.format("%s-%s-readonly", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName));
        model.removeRole(String.format("%s-%s-deployer", RepositoryInstancePrivilegeDescriptor.TYPE, repositoryName));
      }
    });
  }
}
