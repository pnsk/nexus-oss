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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.util.LinkedList;

import javax.annotation.Nullable;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * A utility class for parsing the repository name and remaining path out of a request URI.
 *
 * @since 3.0
 */
class RepositoryPath
{
  private final String repositoryName;

  private final String remainingPath;

  private RepositoryPath(final String repositoryName, final String remainingPath) {
    this.repositoryName = repositoryName;
    this.remainingPath = remainingPath;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getRemainingPath() {
    return remainingPath;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "repositoryName='" + repositoryName + '\'' +
        ", remainingPath='" + remainingPath + '\'' +
        '}';
  }

  //
  // Parser
  //

  /**
   * @return The parsed path or {@code null}
   */
  @Nullable
  public static RepositoryPath parse(final @Nullable String input) {
    // input not be null or empty
    if (input == null || input.isEmpty()) {
      return null;
    }

    // input must start with '/'
    if (!(input.charAt(0) == '/')) {
      return null;
    }

    // input must have another '/' after initial '/'
    int i = input.indexOf('/', 1);
    if (i == -1) {
      return null;
    }

    String repo = input.substring(1, i);
    if (repo.equals(".") || repo.equals("..")) {
      return null;
    }

    String path = normalize(input.substring(i, input.length()));

    // if normalization succeeded return success
    if (path != null) {
      return new RepositoryPath(repo, path);
    }

    // otherwise path is invalid
    return null;
  }

  private static final Splitter splitter = Splitter.on('/');

  @Nullable
  private static String normalize(final String input) {
    // root path
    if (input.equals("/")) {
      return input;
    }

    // Parts stack
    LinkedList<String> parts = Lists.newLinkedList();

    // split up string into parts, ignoring first '/'
    for (String part : splitter.split(input.substring(1, input.length()))) {
      if (part.isEmpty()) {
        // empty parts not allowed (ie. // bad)
        return null;
      }
      else if (part.equals(".")) {
        // skip
        continue;
      }
      else if (part.equals("..")) {
        // past stack abort
        if (parts.isEmpty()) {
          return null;
        }
        parts.pop();
      }
      else {
        parts.add(part);
      }
    }

    // rebuild path from normalized parts
    StringBuilder buff = new StringBuilder();
    for (String part : parts) {
      buff.append('/');
      buff.append(part);
    }
    return buff.toString();
  }
}
