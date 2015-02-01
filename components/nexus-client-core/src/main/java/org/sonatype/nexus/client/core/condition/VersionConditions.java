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
package org.sonatype.nexus.client.core.condition;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.NexusStatus;
import org.sonatype.nexus.client.internal.version.GenericVersionScheme;
import org.sonatype.nexus.client.internal.version.InvalidVersionSpecificationException;
import org.sonatype.nexus.client.internal.version.Version;
import org.sonatype.nexus.client.internal.version.VersionConstraint;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Condition}s that matches remote Nexus version.
 *
 * @since 2.1
 */
public abstract class VersionConditions
    implements Condition
{
  /**
   * Version constraint that matches all released Nexus versions beginning with version 1.9.
   */
  private static final VersionConstraint POST_1_8_VERSIONS = parseVersionConstraint("(1.8.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.0.
   */
  private static final VersionConstraint POST_1_9_VERSIONS = parseVersionConstraint("(1.9.99,)");

  public static Condition anyModernVersion() {
    return new VersionCondition(POST_1_8_VERSIONS);
  }

  public static Condition any20AndLaterVersion() {
    return new VersionCondition(POST_1_9_VERSIONS);
  }

  public static Condition withVersion(final String versionRange) {
    return new VersionCondition(parseVersionConstraint(versionRange));
  }

  private static class VersionCondition
      implements Condition
  {

    private final VersionConstraint suitableVersions;

    private VersionCondition(final VersionConstraint suitableVersions) {
      this.suitableVersions = checkNotNull(suitableVersions);
    }

    /**
     * Returns true if both, editionShort and version matches the given constraints.
     */
    public boolean isSatisfiedBy(final NexusStatus status) {
      final Version version = parseVersion(status.getVersion());
      return suitableVersions.containsVersion(version);
    }

    @Override
    public String explainNotSatisfied(final NexusStatus status) {
      return String.format("(version \"%s\" contained in \"%s\")", status.getVersion(), suitableVersions);
    }
  }

  private static VersionConstraint parseVersionConstraint(final String versionConstraint) {
    try {
      return new GenericVersionScheme().parseVersionConstraint(versionConstraint);
    }
    catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException("Unable to parse version constraint: " + versionConstraint, e);
    }
  }

  private static Version parseVersion(final String version) {
    try {
      return new GenericVersionScheme().parseVersion(version);
    }
    catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException("Unable to parse version: " + version, e);
    }
  }
}