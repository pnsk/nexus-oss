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
package org.sonatype.nexus.common.hash;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link HashCode} paired with a {@link HashAlgorithm}.
 *
 * @since 3.0
 */
public class Hash
{
  private final HashCode code;

  private final HashAlgorithm algorithm;

  public Hash(HashCode code, HashAlgorithm algorithm) {
    this.code = checkNotNull(code);
    this.algorithm = checkNotNull(algorithm);
  }

  public HashCode code() {
    return code;
  }

  public HashAlgorithm algorithm() {
    return algorithm;
  }
}
