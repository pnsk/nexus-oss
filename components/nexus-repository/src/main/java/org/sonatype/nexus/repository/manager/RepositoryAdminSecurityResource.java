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

package org.sonatype.nexus.repository.manager;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.security.model.CRoleBuilder;
import org.sonatype.security.model.SecurityModelConfiguration;
import org.sonatype.security.realms.tools.MutableDynamicSecurityResource;

import static org.sonatype.nexus.repository.security.BreadActions.ADD;
import static org.sonatype.nexus.repository.security.BreadActions.BROWSE;
import static org.sonatype.nexus.repository.security.BreadActions.DELETE;
import static org.sonatype.nexus.repository.security.BreadActions.EDIT;
import static org.sonatype.nexus.repository.security.BreadActions.READ;
import static org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor.TYPE;
import static org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor.id;
import static org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor.privilege;

/**
 * Repository administration security resource.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RepositoryAdminSecurityResource
    extends MutableDynamicSecurityResource
{
  public RepositoryAdminSecurityResource() {
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        initial(model);
      }
    });
  }

  // TODO: Sort out role[-naming] scheme

  private void initial(final SecurityModelConfiguration model) {
    model.addPrivilege(privilege("*", BROWSE));
    model.addPrivilege(privilege("*", READ));
    model.addPrivilege(privilege("*", EDIT));
    model.addPrivilege(privilege("*", ADD));
    model.addPrivilege(privilege("*", DELETE));

    model.addRole(new CRoleBuilder()
        .id(String.format("%s-fullcontrol", TYPE))
        .privilege(id("*", BROWSE))
        .privilege(id("*", READ))
        .privilege(id("*", EDIT))
        .privilege(id("*", ADD))
        .privilege(id("*", DELETE))
        .create());
  }

  public void add(final Repository repository) {
    final String repositoryName = repository.getName();
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        model.addPrivilege(privilege(repositoryName, BROWSE));
        model.addPrivilege(privilege(repositoryName, READ));
        model.addPrivilege(privilege(repositoryName, EDIT));
        // no per-repo repository-admin ADD action
        model.addPrivilege(privilege(repositoryName, DELETE));

        model.addRole(new CRoleBuilder()
            .id(String.format("%s-%s-fullcontrol", repositoryName, TYPE))
            .privilege(id(repositoryName, BROWSE))
            .privilege(id(repositoryName, READ))
            .privilege(id(repositoryName, EDIT))
                // no per-repo repository-admin ADD action
            .privilege(id(repositoryName, DELETE))
            .create());
      }
    });
  }

  public void remove(final Repository repository) {
    final String repositoryName = repository.getName();
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        model.removePrivilege(id(repositoryName, BROWSE));
        model.removePrivilege(id(repositoryName, READ));
        model.removePrivilege(id(repositoryName, EDIT));
        // no per-repo repository-admin ADD action
        model.removePrivilege(id(repositoryName, DELETE));

        model.removeRole(String.format("%s-%s-fullcontrol", repositoryName, TYPE));
      }
    });
  }
}
