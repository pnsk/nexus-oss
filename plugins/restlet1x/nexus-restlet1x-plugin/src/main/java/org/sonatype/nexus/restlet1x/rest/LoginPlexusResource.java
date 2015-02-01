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
package org.sonatype.nexus.restlet1x.rest;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.restlet1x.model.AuthenticationLoginResource;
import org.sonatype.nexus.restlet1x.model.AuthenticationLoginResourceResponse;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Login resource.
 */
@Named
@Singleton
@Path(LoginPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class LoginPlexusResource
    extends AbstractPlexusResource
{
  public static final String RESOURCE_URI = "/authentication/login";

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    // this is the ONLY resource using authcNxBasic, as the UI can't receive 401 errors from teh server
    // as the browser login pops up, which is no good in this case
    return new PathProtectionDescriptor(getResourceUri(), "authcNxBasic,perms[nexus:authentication]");
  }

  @Override
  public AuthenticationLoginResourceResponse get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    AuthenticationLoginResource resource = new AuthenticationLoginResource();
    AuthenticationLoginResourceResponse result = new AuthenticationLoginResourceResponse();
    result.setData(resource);

    return result;
  }
}
