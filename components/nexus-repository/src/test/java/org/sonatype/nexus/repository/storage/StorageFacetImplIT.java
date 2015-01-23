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

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.orient.DatabaseInstanceRule;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.inject.util.Providers;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_PATH;
import static org.sonatype.nexus.repository.storage.StorageFacet.V_ASSET;
import static org.sonatype.nexus.repository.storage.StorageFacet.V_BUCKET;
import static org.sonatype.nexus.repository.storage.StorageFacet.V_COMPONENT;

/**
 * Integration tests for {@link StorageFacetImpl}.
 */
public class StorageFacetImplIT
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = new DatabaseInstanceRule("test");

  protected StorageFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    BlobStoreManager mockBlobStoreManager = mock(BlobStoreManager.class);
    when(mockBlobStoreManager.get(anyString())).thenReturn(mock(BlobStore.class));
    underTest = new StorageFacetImpl(
        mockBlobStoreManager,
        Providers.of(database.getInstance())
    );
    underTest.installDependencies(mock(EventBus.class));
    Repository mockRepository = mock(Repository.class);
    when(mockRepository.getName()).thenReturn("test-repository");
    underTest.init(mockRepository);
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();
  }

  @Test
  public void initialState() {
    try (StorageTx tx = underTest.openTx()) {
      // We should have one bucket, which was auto-created for the repository during initialization
      checkSize(tx.browseVertices(null), 1);
      checkSize(tx.browseVertices(V_BUCKET), 1);
    }
  }

  @Test
  public void mapOfMaps() {
    Map<String, String> bag1 = ImmutableMap.of("foo", "bar");
    Map<String, String> bag2 = ImmutableMap.of("baz", "qux");
    Map<String, Map<String, String>> inputMap = ImmutableMap.of("bag1", bag1, "bag2", bag2);

    // Transaction 1:
    // Create a new asset with property "attributes" that's a map of maps (stored as an embeddedmap)
    Object vertexId;
    try (StorageTx tx = underTest.openTx()) {
      Vertex bucket = tx.getBucket();
      OrientVertex asset = tx.createAsset(bucket);
      asset.setProperty("attributes", inputMap, OType.EMBEDDEDMAP);
      tx.commit();
      vertexId = asset.getId();
    }

    // Transaction 2:
    // Get the asset and make sure it contains what we expect
    try (StorageTx tx = underTest.openTx()) {
      Vertex bucket = tx.getBucket();
      OrientVertex asset = tx.findVertex(vertexId, null);
      assert asset != null;

      Map<String, Map<String, String>> outputMap = asset.getProperty("attributes");

      assertThat(outputMap.keySet().size(), is(2));

      Map<String, String> outputBag1 = outputMap.get("bag1");
      assertNotNull(outputBag1);
      assertThat(outputBag1.keySet().size(), is(1));
      assertThat(outputBag1.get("foo"), is("bar"));

      Map<String, String> outputBag2 = outputMap.get("bag2");
      assertNotNull(outputBag2);
      assertThat(outputBag2.keySet().size(), is(1));
      assertThat(outputBag2.get("baz"), is("qux"));
    }

    // Transaction 3:
    // Make sure we can use dot notation to query for the asset by some aspect of the attributes
    try (StorageTx tx = underTest.openTx()) {
      Map<String, String> parameters = ImmutableMap.of("fooValue", "bar");
      String query = String.format("select from %s where attributes.bag1.foo = :fooValue", V_ASSET);

      Iterable<OrientVertex> vertices = tx.getGraphTx().command(new OCommandSQL(query)).execute(parameters);
      List<OrientVertex> list = Lists.newArrayList(vertices);

      assertThat(list.size(), is(1));
      assertThat(list.get(0).getId(), is(vertexId));
    }
  }

  @Test
  public void roundTripTest() {
    try (StorageTx tx = underTest.openTx()) {
      // Make two buckets and verify state with browse and find
      Vertex bucket1 = tx.createVertex(V_BUCKET);
      Vertex bucket2 = tx.createVertex(V_BUCKET);

      checkSize(tx.browseVertices(null), 3);
      checkSize(tx.browseVertices(V_BUCKET), 3);
      checkSize(tx.browseVertices(V_ASSET), 0);
      checkSize(tx.browseVertices(V_COMPONENT), 0);

      assertNotNull(tx.findVertex(bucket1.getId(), null));
      assertNotNull(tx.findVertex(bucket2.getId(), null));
      assertNotNull(tx.findVertex(bucket1.getId(), V_BUCKET));
      assertNotNull(tx.findVertex(bucket2.getId(), V_BUCKET));
      assertNull(tx.findVertex(bucket1.getId(), V_ASSET));
      assertNull(tx.findVertex(bucket2.getId(), V_ASSET));

      // Create an asset and component, one in each bucket, and verify state with browse and find
      Vertex asset = tx.createAsset(bucket1);
      asset.setProperty(P_PATH, "path");
      Vertex component = tx.createComponent(bucket2);
      component.setProperty("foo", "bar");

      checkSize(tx.browseVertices(V_ASSET), 1);
      checkSize(tx.browseVertices(V_COMPONENT), 1);
      checkSize(tx.browseVertices(V_ASSET), 1);
      checkSize(tx.browseAssets(bucket1), 1);
      checkSize(tx.browseAssets(bucket2), 0);
      checkSize(tx.browseComponents(bucket1), 0);
      checkSize(tx.browseComponents(bucket2), 1);

      assertNotNull(tx.findVertex(asset.getId(), V_ASSET));
      assertNotNull(tx.findVertex(component.getId(), V_COMPONENT));

      assertNull(tx.findVertexWithProperty(P_PATH, "nomatch", null));
      assertNotNull(tx.findVertexWithProperty(P_PATH, "path", null));
      assertNotNull(tx.findVertexWithProperty(P_PATH, "path", V_ASSET));
      assertNotNull(tx.findAssetWithProperty(P_PATH, "path", bucket1));
      assertNull(tx.findAssetWithProperty(P_PATH, "nomatch", bucket1));
      assertNull(tx.findAssetWithProperty(P_PATH, "path", bucket2));
      assertNull(tx.findVertex(bucket2.getId(), V_ASSET));

      assertNull(tx.findVertexWithProperty("foo", "nomatch", null));
      assertNotNull(tx.findVertexWithProperty("foo", "bar", null));
      assertNotNull(tx.findVertexWithProperty("foo", "bar", V_COMPONENT));
      assertNotNull(tx.findComponentWithProperty("foo", "bar", bucket2));
      assertNull(tx.findComponentWithProperty("foo", "nomatch", bucket2));
      assertNull(tx.findComponentWithProperty("foo", "bar", bucket1));
      assertNull(tx.findVertex(bucket2.getId(), V_COMPONENT));

      // Delete both and make sure browse and find behave as expected
      tx.deleteVertex(asset);
      tx.deleteVertex(component);

      checkSize(tx.browseVertices(V_ASSET), 0);
      checkSize(tx.browseVertices(V_COMPONENT), 0);
      assertNull(tx.findVertex(asset.getId(), V_ASSET));
      assertNull(tx.findVertex(component.getId(), V_COMPONENT));

      // NOTE: It doesn't matter for this test, but you should commit when finished with one or more writes
      //       If you don't, your changes will be automatically rolled back.
      tx.commit();
    }
  }

  private void checkSize(Iterable iterable, int expectedSize) {
    assertThat(Iterators.size(iterable.iterator()), is(expectedSize));
  }
}
