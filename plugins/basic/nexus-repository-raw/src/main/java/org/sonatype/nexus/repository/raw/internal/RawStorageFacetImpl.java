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

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.raw.RawContent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.collect.ImmutableMap;
import com.tinkerpop.blueprints.Vertex;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkState;
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
    implements RawStorageFacet
{
  @Inject
  public RawStorageFacetImpl() {
  }

  @Nullable
  @Override
  public RawContent get(final String path) {
    try (StorageTx tx = getStorage().openTx()) {
      final Vertex asset = tx.findAssetWithProperty(P_PATH, path, tx.getBucket());
      if (asset == null) {
        return null;
      }

      final BlobRef blobRef = getBlobRef(path, asset);
      final Blob blob = tx.getBlob(blobRef);
      checkState(blob != null, "asset at path %s refers to missing blob %s", path, blobRef);

      return marshall(asset, blob);
    }
  }

  @Nullable
  @Override
  public RawContent put(final String path, final RawContent content) throws IOException {
    try (StorageTx tx = getStorage().openTx()) {
      Vertex bucket = tx.getBucket();
      Vertex asset = tx.findAssetWithProperty(P_PATH, path, bucket);
      if (asset == null) {
        asset = tx.createAsset(bucket);
        asset.setProperty(P_PATH, path);
      }
      else {
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
      final Vertex asset = tx.findAssetWithProperty(P_PATH, path, tx.getBucket());
      if (asset == null) {
        return false;
      }

      tx.deleteBlob(getBlobRef(path, asset));
      tx.deleteVertex(asset);

      tx.commit();

      return true;
    }
  }

  private StorageFacet getStorage() {
    return getRepository().facet(StorageFacet.class);
  }

  private BlobRef getBlobRef(final String path, final Vertex asset) {
    String blobRefStr = asset.getProperty(P_BLOB_REF);
    checkState(blobRefStr != null, "asset at path %s has missing blob reference", path);
    return BlobRef.parse(blobRefStr);
  }

  private RawContent marshall(final Vertex asset, final Blob blob) {
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
