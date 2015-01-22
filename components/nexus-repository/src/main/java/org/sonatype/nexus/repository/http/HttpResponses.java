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

package org.sonatype.nexus.repository.http;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.PayloadResponse;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import com.google.common.base.Joiner;
import com.google.common.net.HttpHeaders;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.CREATED;
import static org.sonatype.nexus.repository.http.HttpStatus.FORBIDDEN;
import static org.sonatype.nexus.repository.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.NO_CONTENT;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.repository.http.HttpStatus.UNAUTHORIZED;

// TODO: Consider a builder model instead, may be easier to expose more flexibility in terms of custom responses?

/**
 * Convenience methods for constructing various commonly used HTTP responses.
 *
 * @since 3.0
 */
public class HttpResponses
{
  private HttpResponses() {}

  // Ok: 200

  public static Response ok(final @Nullable String message) {
    return new Response(Status.success(OK, message));
  }

  public static Response ok() {
    return ok((String) null);
  }

  public static Response ok(final Payload payload) {
    return new PayloadResponse(Status.success(OK), payload);
  }

  // Created: 201

  public static Response created(final @Nullable String message) {
    return new Response(Status.success(CREATED, message));
  }

  public static Response created() {
    return created(null);
  }

  // No Content: 204

  public static Response noContent(final @Nullable String message) {
    return new Response(Status.success(NO_CONTENT, message));
  }

  public static Response noContent() {
    return noContent(null);
  }

  // Not Found: 404

  public static Response notFound(final @Nullable String message) {
    return new Response(new Status(false, NOT_FOUND, message));
  }

  public static Response notFound() {
    return notFound(null);
  }

  // Bad request: 400

  public static Response badRequest() {
    return new Response(Status.failure(BAD_REQUEST));
  }

  // Unauthorized: 401

  public static Response unauthorized() {
    return new Response(Status.failure(UNAUTHORIZED));
  }

  // Forbidden: 403

  public static Response forbidden() {
    return new Response(Status.failure(FORBIDDEN));
  }

  // Method not allowed: 405

  public static Response methodNotAllowed(final String methodName, final String... allowedMethods) {
    checkNotNull(methodName);
    checkNotNull(allowedMethods);
    checkArgument(allowedMethods.length != 0);
    Response response = new Response(Status.failure(METHOD_NOT_ALLOWED, methodName));
    String allow = Joiner.on(',').join(allowedMethods);
    response.getHeaders().set(HttpHeaders.ALLOW, allow);
    return response;
  }
}
