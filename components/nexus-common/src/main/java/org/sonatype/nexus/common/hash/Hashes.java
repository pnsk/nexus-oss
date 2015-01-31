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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper for computing {@link HashCode}es from {@link InputStream}s.
 *
 * @since 3.0
 */
public final class Hashes
{
  private Hashes() {
    // no instance
  }

  /**
   * Computes the hash of the given stream using the given algorithm.
   */
  public static HashCode hash(InputStream inputStream, HashAlgorithm algorithm) throws IOException {
    checkNotNull(inputStream);
    checkNotNull(algorithm);

    return computeHashes(inputStream, Lists.newArrayList(algorithm)).get(algorithm);
  }

  /**
   * Computes the hash of the given stream using multiple algorithms in one pass.
   */
  public static Map<HashAlgorithm, HashCode> hash(InputStream inputStream, Iterable<HashAlgorithm> algorithms) throws IOException {
    checkNotNull(inputStream);
    checkNotNull(algorithms);

    return computeHashes(inputStream, algorithms);
  }

  private static Map<HashAlgorithm, HashCode> computeHashes(InputStream inputStream, Iterable<HashAlgorithm> algorithms)
      throws IOException {
    // set up chain
    List<HashingInputStream> chain = Lists.newArrayList();
    InputStream lastStream = inputStream;
    for (HashAlgorithm algorithm : algorithms) {
      HashingInputStream currentStream = new HashingInputStream(algorithm.function(), lastStream);
      chain.add(currentStream);
      lastStream = currentStream;
    }

    // send bytes through
    ByteStreams.copy(lastStream, ByteStreams.nullOutputStream());

    // extract hashes in order
    Map<HashAlgorithm, HashCode> hashes = Maps.newHashMap();
    int i = 0;
    for (HashAlgorithm algorithm : algorithms) {
      hashes.put(algorithm, chain.get(i++).hash());
    }

    return hashes;
  }
}
