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
package org.sonatype.nexus.repository.storage;

import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/**
 * A storage transaction.
 *
 * @since 3.0
 */
public interface StorageTx
    extends AutoCloseable
{
  /**
   * Provides the underlying graph transaction.
   *
   * Note: The caller may use this to directly query or manipulate the OrientDB graph, if needed, but should not
   * directly commit, rollback, or close the underlying transaction.
   */
  OrientGraph getGraphTx();

  /**
   * Commits the transaction.
   */
  void commit();

  /**
   * Rolls back the transaction.
   */
  void rollback();

  /**
   * Closes the transaction. Uncommitted changes will be lost, and the object will be ineligible for further use.
   */
  void close();

  /**
   * Gets the bucket for the current repository.
   */
  Vertex getBucket();

  /**
   * Gets all assets owned by the specified bucket.
   */
  Iterable<Vertex> browseAssets(Vertex bucket);

  /**
   * Gets all components owned by the specified bucket.
   */
  Iterable<Vertex> browseComponents(Vertex bucket);

  /**
   * Gets all vertices, optionally limited to those in the specified class.
   */
  Iterable<Vertex> browseVertices(@Nullable String className);

  /**
   * Gets an asset by id, owned by the specified bucket, or {@code null} if not found.
   */
  @Nullable
  Vertex findAsset(Object vertexId, Vertex bucket);

  /**
   * Gets an asset by some identifying property, owned by the specified bucket, or {@code null} if not found.
   */
  @Nullable
  Vertex findAssetWithProperty(String propName, Object propValue, Vertex bucket);

  /**
   * Gets a component by id, owned by the specified bucket, or {@code null} if not found.
   */
  @Nullable
  Vertex findComponent(Object vertexId, Vertex bucket);

  /**
   * Gets a component by some identifying property, or {@code null} if not found.
   */
  @Nullable
  Vertex findComponentWithProperty(String propName, Object propValue, Vertex bucket);

  /**
   * Gets a vertex by id, optionally limited by class, or {@code null} if not found.
   */
  @Nullable
  Vertex findVertex(Object vertexId, @Nullable String className);

  /**
   * Gets a vertex by some identifying property, optionally limited by class, or {@code null} if not found.
   */
  @Nullable
  Vertex findVertexWithProperty(String propName, Object propValue, @Nullable String className);

  /**
   * Creates a new asset owned by the specified bucket.
   */
  Vertex createAsset(Vertex bucket);

  /**
   * Creates a new component owned by the specified bucket.
   */
  Vertex createComponent(Vertex bucket);

  /**
   * Creates a new vertex of the specified class.
   */
  Vertex createVertex(String className);

  /**
   * Deletes an existing vertex.
   */
  void deleteVertex(Vertex vertex);

  /**
   * Creates a new Blob.
   */
  BlobRef createBlob(InputStream inputStream, Map<String, String> headers);

  /**
   * Gets a Blob, or {@code null if not found}.
   */
  @Nullable
  Blob getBlob(BlobRef blobRef);

  /**
   * Deletes a Blob.
   */
  void deleteBlob(BlobRef blobRef);
}
