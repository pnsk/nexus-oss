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

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.restlet1x.model.StatusResource;
import org.sonatype.nexus.restlet1x.model.StatusResourceResponse;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.ManagedPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import com.codahale.metrics.annotation.Timed;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Named("StatusPlexusResource")
@Singleton
@Typed(ManagedPlexusResource.class)
@Path(StatusPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class StatusPlexusResource
    extends AbstractPlexusResource
    implements ManagedPlexusResource
{
  public static final String RESOURCE_URI = "/status";

  private final Provider<SystemStatus> systemStatusProvider;

  @Inject
  public StatusPlexusResource(final Provider<SystemStatus> systemStatusProvider) {
    this.systemStatusProvider = systemStatusProvider;
  }

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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:status]");
  }

  @Timed
  @Override
  @GET
  public StatusResourceResponse get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    final SystemStatus status = systemStatusProvider.get();
    final StatusResource resource = new StatusResource();
    resource.setAppName(status.getAppName());
    resource.setVersion(status.getVersion());
    resource.setEditionShort(status.getEditionShort());

    StatusResourceResponse result = new StatusResourceResponse();
    result.setData(resource);
    return result;
  }
}
