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
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuard;
import org.sonatype.nexus.common.stateguard.StateGuardAware;
import org.sonatype.nexus.common.stateguard.Transitions;
import org.sonatype.nexus.orient.graph.GraphTx;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.storage.StorageFacet.E_OWNS_ASSET;
import static org.sonatype.nexus.repository.storage.StorageFacet.E_OWNS_COMPONENT;
import static org.sonatype.nexus.repository.storage.StorageFacet.V_ASSET;
import static org.sonatype.nexus.repository.storage.StorageFacet.V_COMPONENT;
import static org.sonatype.nexus.repository.storage.StorageTxImpl.State.CLOSED;
import static org.sonatype.nexus.repository.storage.StorageTxImpl.State.OPEN;

/**
 * Default {@link StorageTx} implementation.
 *
 * @since 3.0
 */
public class StorageTxImpl
  extends ComponentSupport
  implements StorageTx, StateGuardAware
{
  private final BlobStoreManager blobStoreManager;

  private final GraphTx graphTx;

  private final Object bucketId;

  private final StateGuard stateGuard = new StateGuard.Builder().initial(CLOSED).create();

  private final Set<BlobRef> newlyCreatedBlobs = Sets.newHashSet();

  private final Set<BlobRef> deletionRequests = Sets.newHashSet();

  public StorageTxImpl(final BlobStoreManager blobStoreManager,
                       final GraphTx graphTx,
                       final Object bucketId) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.graphTx = checkNotNull(graphTx);
    this.bucketId = checkNotNull(bucketId);
  }

  public static final class State
  {
    public static final String OPEN = "OPEN";

    public static final String CLOSED = "CLOSED";
  }

  @Override
  public StateGuard getStateGuard() {
    return stateGuard;
  }

  @Override
  @Guarded(by=OPEN)
  public GraphTx getGraphTx() {
    return graphTx;
  }

  @Override
  @Guarded(by=OPEN)
  public void commit() {
    graphTx.commit();
    doBlobDeletions(deletionRequests, "Unable to delete old blob {} while committing transaction");
    clearBlobState();
  }

  @Override
  @Guarded(by=OPEN)
  public void rollback() {
    graphTx.rollback();
    doBlobDeletions(newlyCreatedBlobs, "Unable to delete new blob {} while explicitly rolling back transaction");
    clearBlobState();
  }

  private void clearBlobState() {
    newlyCreatedBlobs.clear();
    deletionRequests.clear();
  }

  private void doBlobDeletions(Set<BlobRef> blobRefs, String failureMessage) {
    for (BlobRef blobRef : blobRefs) {
      try {
        blobStore().delete(blobRef.getBlobId());
      }
      catch (Throwable t) {
        log.warn(failureMessage, t, blobRef);
      }
    }
  }

  @Override
  @Transitions(from = OPEN, to = CLOSED)
  public void close() {
    graphTx.close(); // rolls back and releases underlying ODatabaseDocumentTx to pool
    doBlobDeletions(newlyCreatedBlobs, "Unable to delete new blob {} while implicitly rolling back transaction");
  }

  @Override
  @Guarded(by=OPEN)
  public Vertex getBucket() {
    return findVertex(bucketId, null);
  }

  @Override
  @Guarded(by=OPEN)
  public Iterable<Vertex> browseAssets(final Vertex bucket) {
    checkNotNull(bucket);

    return bucket.getVertices(Direction.OUT, E_OWNS_ASSET);
  }

  @Override
  @Guarded(by=OPEN)
  public Iterable<Vertex> browseComponents(final Vertex bucket) {
    checkNotNull(bucket);

    return bucket.getVertices(Direction.OUT, E_OWNS_COMPONENT);
  }

  @Override
  @Guarded(by=OPEN)
  public Iterable<Vertex> browseVertices(@Nullable final String className) {
    if (className == null) {
      return graphTx.getVertices();
    }
    else {
      return graphTx.getVerticesOfClass(className);
    }
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public Vertex findAsset(final Object vertexId, final Vertex bucket) {
    checkNotNull(vertexId);
    checkNotNull(bucket);

    Vertex vertex = findVertex(vertexId, V_ASSET);
    return bucketOwns(bucket, E_OWNS_ASSET, vertex) ? vertex : null;
  }

  private boolean bucketOwns(Vertex bucket, String edgeLabel, @Nullable Vertex item) {
    if (item == null) {
      return false;
    }
    Vertex first = Iterables.getFirst(item.getVertices(Direction.IN, edgeLabel), null);
    return bucket.equals(first);
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public Vertex findAssetWithProperty(final String propName, final Object propValue,
                                      final Vertex bucket)
  {
    return findWithPropertyOwnedBy(V_ASSET, propName, propValue, E_OWNS_ASSET, bucket);
  }

  @SuppressWarnings("unchecked")
  private Vertex findWithPropertyOwnedBy(String className, String propName, Object propValue,
                                         String edgeLabel, Vertex bucket) {
    checkNotNull(propName);
    checkNotNull(propValue);
    checkNotNull(bucket);

    Map<String, Object> parameters = ImmutableMap.of("propValue", propValue, "bucket", bucket);
    String query = String.format("select from %s where %s = :propValue and in('%s') contains :bucket",
        className, propName, edgeLabel);
    Iterable<Vertex> vertices = (Iterable<Vertex>) graphTx.command(new OCommandSQL(query)).execute(parameters);
    return Iterables.getFirst(vertices, null);
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public Vertex findComponent(final Object vertexId, final Vertex bucket) {
    checkNotNull(vertexId);
    checkNotNull(bucket);

    Vertex vertex = findVertex(vertexId, V_COMPONENT);
    return bucketOwns(bucket, E_OWNS_COMPONENT, vertex) ? vertex : null;
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public Vertex findComponentWithProperty(final String propName, final Object propValue, final Vertex bucket) {
    return findWithPropertyOwnedBy(V_COMPONENT, propName, propValue, E_OWNS_COMPONENT, bucket);
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public Vertex findVertex(final Object vertexId, @Nullable final String className) {
    checkNotNull(vertexId);

    Vertex vertex = graphTx.getVertex(vertexId);
    if (vertex != null && className != null && !vertex.getProperty("@class").equals(className)) {
      return null;
    }
    return vertex;
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public Vertex findVertexWithProperty(final String propName, final Object propValue,
                                       @Nullable final String className) {
    checkNotNull(propName);
    checkNotNull(propValue);

    Vertex vertex = Iterables.getFirst(graphTx.getVertices(propName, propValue), null);
    if (vertex != null && className != null && !vertex.getProperty("@class").equals(className)) {
      return null;
    }
    return vertex;
  }

  @Override
  @Guarded(by=OPEN)
  public Vertex createAsset(final Vertex bucket) {
    checkNotNull(bucket);

    Vertex asset = createVertex(V_ASSET);
    graphTx.addEdge(null, bucket, asset, E_OWNS_ASSET);
    return asset;
  }

  @Override
  @Guarded(by=OPEN)
  public Vertex createComponent(final Vertex bucket) {
    checkNotNull(bucket);

    Vertex component = createVertex(V_COMPONENT);
    graphTx.addEdge(null, bucket, component, E_OWNS_COMPONENT);
    return component;
  }

  @Override
  @Guarded(by=OPEN)
  public Vertex createVertex(final String className) {
    checkNotNull(className);

    return graphTx.addVertex(className, (String) null);
  }

  @Override
  @Guarded(by=OPEN)
  public void deleteVertex(final Vertex vertex) {
    checkNotNull(vertex);

    graphTx.removeVertex(vertex);
  }

  private BlobStore blobStore() {
    return blobStoreManager.get("default");
  }

  @Override
  @Guarded(by=OPEN)
  public BlobRef createBlob(final InputStream inputStream, Map<String, String> headers) {
    checkNotNull(inputStream);
    checkNotNull(headers);

    Blob blob = blobStore().create(inputStream, headers);
    BlobRef blobRef = new BlobRef("NODE", "STORE", blob.getId().asUniqueString());
    newlyCreatedBlobs.add(blobRef);
    return blobRef;
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public Blob getBlob(final BlobRef blobRef) {
    checkNotNull(blobRef);

    return blobStore().get(blobRef.getBlobId());
  }

  @Override
  @Guarded(by=OPEN)
  public void deleteBlob(final BlobRef blobRef) {
    checkNotNull(blobRef);

    deletionRequests.add(blobRef);
  }
}
