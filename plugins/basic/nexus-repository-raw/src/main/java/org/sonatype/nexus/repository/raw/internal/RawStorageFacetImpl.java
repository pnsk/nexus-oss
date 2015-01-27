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
package org.sonatype.nexus.repository.raw.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.negativecache.NegativeCacheKey;
import org.sonatype.nexus.repository.negativecache.NegativeCacheKeySource;
import org.sonatype.nexus.repository.raw.RawContent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Context;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.repository.storage.StorageFacet.E_PART_OF_COMPONENT;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_BLOB_REF;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_CONTENT_TYPE;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_LAST_MODIFIED;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_PATH;

/**
 * A {@link RawStorageFacet} that persists to a {@link StorageFacet}.
 *
 * @since 3.0
 */
public class RawStorageFacetImpl
    extends FacetSupport
    implements RawStorageFacet, NegativeCacheKeySource
{
  @Inject
  public RawStorageFacetImpl() {
  }

  @Nullable
  @Override
  public RawContent get(final String path) {
    try (StorageTx tx = getStorage().openTx()) {
      final OrientVertex component = tx.findComponentWithProperty(P_PATH, path, tx.getBucket());
      if (component == null) {
        return null;
      }

      final OrientVertex asset = getAsset(component);
      final BlobRef blobRef = getBlobRef(path, asset);
      final Blob blob = tx.getBlob(blobRef);
      checkState(blob != null, "asset of component with at path %s refers to missing blob %s", path, blobRef);

      return marshall(asset, blob);
    }
  }

  @Nullable
  @Override
  public RawContent put(final String path, final RawContent content) throws IOException {
    try (StorageTx tx = getStorage().openTx()) {
      OrientVertex bucket = tx.getBucket();
      OrientVertex component = tx.findComponentWithProperty(P_PATH, path, bucket);
      OrientVertex asset;
      if (component == null) {
        // CREATE
        component = tx.createComponent(bucket);

        // TODO: Set common component props at top level, set other props as format-specific attributes?
        component.setProperty(P_PATH, path);

        asset = tx.createAsset(bucket);
        asset.addEdge(E_PART_OF_COMPONENT, component);
      }
      else {
        // UPDATE
        asset = getAsset(component);
        final BlobRef oldBlobRef = getBlobRef(path, asset);
        tx.deleteBlob(oldBlobRef);
      }

      // TODO: Figure out created-by header
      final ImmutableMap<String, String> headers = ImmutableMap
          .of(BlobStore.BLOB_NAME_HEADER, path, BlobStore.CREATED_BY_HEADER, "unknown");

      final BlobRef newBlobRef = tx.createBlob(content.openInputStream(), headers);

      asset.setProperty(P_BLOB_REF, newBlobRef.toString());
      if (content.getContentType() != null) {
        asset.setProperty(P_CONTENT_TYPE, content.getContentType());
      }

      final DateTime lastModified = content.getLastModified();
      if (lastModified != null) {
        asset.setProperty(P_LAST_MODIFIED, new Date(lastModified.getMillis()));
      }

      tx.commit();

      return marshall(asset, tx.getBlob(newBlobRef));
    }
  }


  @Override
  public boolean delete(final String path) throws IOException {
    try (StorageTx tx = getStorage().openTx()) {
      final OrientVertex component = tx.findComponentWithProperty(P_PATH, path, tx.getBucket());
      if (component == null) {
        return false;
      }
      OrientVertex asset = getAsset(component);

      tx.deleteBlob(getBlobRef(path, asset));
      tx.deleteVertex(asset);
      tx.deleteVertex(component);

      tx.commit();

      return true;
    }
  }

  @Override
  public NegativeCacheKey cacheKey(final Context context) {
    return new NegativeCacheKey(context.getRequest().getPath());
  }

  private StorageFacet getStorage() {
    return getRepository().facet(StorageFacet.class);
  }

  private BlobRef getBlobRef(final String path, final OrientVertex asset) {
    String blobRefStr = asset.getProperty(P_BLOB_REF);
    checkState(blobRefStr != null, "asset of component at path %s has missing blob reference", path);
    return BlobRef.parse(blobRefStr);
  }

  private OrientVertex getAsset(OrientVertex component) {
    List<Vertex> vertices = Lists.newArrayList(component.getVertices(Direction.IN, E_PART_OF_COMPONENT));
    checkState(!vertices.isEmpty());
    return (OrientVertex) vertices.get(0);
  }

  private RawContent marshall(final OrientVertex asset, final Blob blob) {
    final String contentType = asset.getProperty(P_CONTENT_TYPE);

    final Date date = asset.getProperty(P_LAST_MODIFIED);
    final DateTime lastModified = date == null ? null : new DateTime(date.getTime());

    return new RawContent()
    {
      @Override
      public String getContentType() {
        return contentType;
      }

      @Override
      public long getSize() {
        return blob.getMetrics().getContentSize();
      }

      @Override
      public InputStream openInputStream() {
        return blob.getInputStream();
      }

      @Override
      public DateTime getLastModified() {
        return lastModified;
      }
    };
  }
}
