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
package org.sonatype.nexus.client.core;

/**
 * Status of the remote Nexus instance.
 *
 * @since 2.1
 */
public class NexusStatus
{
  private final String appName;

  private final String version;

  private final String editionShort;

  private final String state;

  public NexusStatus(final String appName,
                     final String version,
                     final String editionShort,
                     final String state)
  {
    this.appName = appName;
    this.version = version;
    this.editionShort = editionShort;
    this.state = state;
  }

  public String getVersion() {
    return version;
  }

  public String getEditionShort() {
    return editionShort;
  }

  public String getState() {
    return state;
  }

  @Override
  public String toString() {
    return "NexusStatus [appName=" + appName + ", version=" + version + ", editionShort=" + editionShort + "]";
  }
}
