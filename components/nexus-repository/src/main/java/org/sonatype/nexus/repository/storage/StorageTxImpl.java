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
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuard;
import org.sonatype.nexus.common.stateguard.StateGuardAware;
import org.sonatype.nexus.common.stateguard.Transitions;
import org.sonatype.nexus.orient.graph.GraphTx;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

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
  private final BlobTx blobTx;

  private final GraphTx graphTx;

  private final Object bucketId;

  private final StateGuard stateGuard = new StateGuard.Builder().initial(CLOSED).create();

  public StorageTxImpl(final BlobTx blobTx,
                       final GraphTx graphTx,
                       final Object bucketId) {
    this.blobTx = checkNotNull(blobTx);
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
    blobTx.commit();
  }

  @Override
  @Guarded(by=OPEN)
  public void rollback() {
    graphTx.rollback();
    blobTx.rollback();
  }

  @Override
  @Transitions(from = OPEN, to = CLOSED)
  public void close() {
    graphTx.close(); // rolls back and releases underlying ODatabaseDocumentTx to pool
    blobTx.rollback(); // no-op if no changes have occurred since last commit
  }

  @Override
  @Guarded(by=OPEN)
  public OrientVertex getBucket() {
    return findVertex(bucketId, null);
  }

  @Override
  @Guarded(by=OPEN)
  public Iterable<OrientVertex> browseAssets(final Vertex bucket) {
    checkNotNull(bucket);

    return orientVertices(bucket.getVertices(Direction.OUT, E_OWNS_ASSET));
  }

  @Override
  @Guarded(by=OPEN)
  public Iterable<OrientVertex> browseComponents(final Vertex bucket) {
    checkNotNull(bucket);

    return orientVertices(bucket.getVertices(Direction.OUT, E_OWNS_COMPONENT));
  }

  @Override
  @Guarded(by=OPEN)
  public Iterable<OrientVertex> browseVertices(@Nullable final String className) {
    if (className == null) {
      return orientVertices(graphTx.getVertices());
    }
    else {
      return orientVertices(graphTx.getVerticesOfClass(className));
    }
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public OrientVertex findAsset(final Object vertexId, final Vertex bucket) {
    checkNotNull(vertexId);
    checkNotNull(bucket);

    OrientVertex vertex = findVertex(vertexId, V_ASSET);
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
  public OrientVertex findAssetWithProperty(final String propName, final Object propValue,
                                            final Vertex bucket)
  {
    return findWithPropertyOwnedBy(V_ASSET, propName, propValue, E_OWNS_ASSET, bucket);
  }

  @SuppressWarnings("unchecked")
  private OrientVertex findWithPropertyOwnedBy(String className, String propName, Object propValue,
                                               String edgeLabel, Vertex bucket) {
    checkNotNull(propName);
    checkNotNull(propValue);
    checkNotNull(bucket);

    Map<String, Object> parameters = ImmutableMap.of("propValue", propValue, "bucket", bucket);
    String query = String.format("select from %s where %s = :propValue and in('%s') contains :bucket",
        className, propName, edgeLabel);
    Iterable<OrientVertex> vertices = graphTx.command(new OCommandSQL(query)).execute(parameters);
    return Iterables.getFirst(vertices, null);
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public OrientVertex findComponent(final Object vertexId, final Vertex bucket) {
    checkNotNull(vertexId);
    checkNotNull(bucket);

    OrientVertex vertex = findVertex(vertexId, V_COMPONENT);
    return bucketOwns(bucket, E_OWNS_COMPONENT, vertex) ? vertex : null;
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public OrientVertex findComponentWithProperty(final String propName, final Object propValue, final Vertex bucket) {
    return findWithPropertyOwnedBy(V_COMPONENT, propName, propValue, E_OWNS_COMPONENT, bucket);
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public OrientVertex findVertex(final Object vertexId, @Nullable final String className) {
    checkNotNull(vertexId);

    OrientVertex vertex = graphTx.getVertex(vertexId);
    if (vertex != null && className != null && !vertex.getProperty("@class").equals(className)) {
      return null;
    }
    return vertex;
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public OrientVertex findVertexWithProperty(final String propName, final Object propValue,
                                             @Nullable final String className) {
    checkNotNull(propName);
    checkNotNull(propValue);

    Iterable<OrientVertex> vertices = orientVertices(graphTx.getVertices(propName, propValue));
    OrientVertex vertex = Iterables.getFirst(vertices, null);
    if (vertex != null && className != null && !vertex.getProperty("@class").equals(className)) {
      return null;
    }
    return vertex;
  }

  @Override
  @Guarded(by=OPEN)
  public OrientVertex createAsset(final Vertex bucket) {
    checkNotNull(bucket);

    OrientVertex asset = createVertex(V_ASSET);
    graphTx.addEdge(null, bucket, asset, E_OWNS_ASSET);
    return asset;
  }

  @Override
  @Guarded(by=OPEN)
  public OrientVertex createComponent(final Vertex bucket) {
    checkNotNull(bucket);

    OrientVertex component = createVertex(V_COMPONENT);
    graphTx.addEdge(null, bucket, component, E_OWNS_COMPONENT);
    return component;
  }

  @Override
  @Guarded(by=OPEN)
  public OrientVertex createVertex(final String className) {
    checkNotNull(className);

    return graphTx.addVertex(className, (String) null);
  }

  @Override
  @Guarded(by=OPEN)
  public void deleteVertex(final Vertex vertex) {
    checkNotNull(vertex);

    graphTx.removeVertex(vertex);
  }

  @Override
  @Guarded(by=OPEN)
  public BlobRef createBlob(final InputStream inputStream, Map<String, String> headers) {
    checkNotNull(inputStream);
    checkNotNull(headers);

    return blobTx.create(inputStream, headers);
  }

  @Nullable
  @Override
  @Guarded(by=OPEN)
  public Blob getBlob(final BlobRef blobRef) {
    checkNotNull(blobRef);

    return blobTx.get(blobRef);
  }

  @Override
  @Guarded(by=OPEN)
  public void deleteBlob(final BlobRef blobRef) {
    checkNotNull(blobRef);

    blobTx.delete(blobRef);
  }

  private static Iterable<OrientVertex> orientVertices(Iterable<Vertex> plainVertices) {
    return Iterables.transform(plainVertices, new Function<Vertex, OrientVertex>() {
      @Override
      public OrientVertex apply(final Vertex vertex) {
        return (OrientVertex) vertex;
      }
    });
  }
}
