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

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.security.BreadActions;
import org.sonatype.nexus.repository.security.RepositoryFormatPrivilegeDescriptor;
import org.sonatype.nexus.repository.security.RepositoryInstancePrivilegeDescriptor;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * Simple security handler.
 *
 * @since 3.0
 */
@Named
@Singleton
public class SimpleSecurityHandler
  extends ComponentSupport
  implements Handler
{
  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    // lookup the subject to verify permissions on
    Subject subject = SecurityUtils.getSubject();

    // determine permission action from request
    String action = action(context.getRequest());

    // subject must have either format or instance permissions
    Repository repository = context.getRepository();
    String formatPerm = RepositoryFormatPrivilegeDescriptor.permission(repository.getFormat().getValue(), action);
    String instancePerm = RepositoryInstancePrivilegeDescriptor.permission(repository.getName(), action);

    SimpleSecurityFacet securityFacet = repository.facet(SimpleSecurityFacet.class);
    if (securityFacet.anyPermitted(subject, formatPerm, instancePerm)) {
      // TODO: Handle security exception
      return context.proceed();
    }

    return HttpResponses.unauthorized();
  }

  /**
   * Returns BREAD action for request action.
   */
  private String action(final Request request) {
    switch (request.getAction()) {
      case HttpMethods.OPTIONS:
      case HttpMethods.GET:
      case HttpMethods.HEAD:
      case HttpMethods.TRACE:
        return BreadActions.READ;

      case HttpMethods.POST:
        return BreadActions.ADD;

      case HttpMethods.PUT:
        return BreadActions.EDIT;

      case HttpMethods.DELETE:
        return BreadActions.DELETE;
    }

    throw new RuntimeException("Unsupported action: " + request.getAction());
  }
}
