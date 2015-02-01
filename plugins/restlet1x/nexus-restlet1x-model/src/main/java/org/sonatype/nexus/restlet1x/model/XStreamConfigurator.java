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
package org.sonatype.nexus.restlet1x.model;

import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import com.thoughtworks.xstream.XStream;

/**
 * XStream configurator.
 */
public class XStreamConfigurator
{
  public static XStream configureXStream(XStream xstream) {
    return configureXStream(xstream, ErrorResponse.class, ErrorMessage.class);
  }

  public static XStream configureXStream(XStream xstream, Class<?> errorResponseClazz, Class<?> errorMessageClazz) {
    // protect against XSS, escape HTML from input.
    xstream.registerConverter(new HtmlEscapeStringConverter());

    xstream.processAnnotations(StatusResourceResponse.class);

    xstream.alias("nexus-error", errorResponseClazz);
    xstream.alias("error", errorMessageClazz);
    xstream.registerLocalConverter(errorResponseClazz, "errors", new AliasingListConverter(errorMessageClazz, "error"));

    xstream.alias("authentication-login", AuthenticationLoginResourceResponse.class);

    xstream.registerLocalConverter(AuthenticationClientPermissions.class, "permissions",
        new AliasingListConverter(ClientPermission.class, "permission"));

    xstream.registerLocalConverter(StatusConfigurationValidationResponse.class, "validationErrors",
        new AliasingListConverter(String.class, "error"));
    xstream.registerLocalConverter(StatusConfigurationValidationResponse.class, "validationWarnings",
        new AliasingListConverter(String.class, "warning"));

    xstream.processAnnotations(AuthenticationLoginResourceResponse.class);

    return xstream;
  }
}
