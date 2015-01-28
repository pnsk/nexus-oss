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
package org.sonatype.nexus.yum.internal.createrepo;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.common.collect.Maps;

/**
 * @since 3.0
 */
@Named
@Singleton
public class YumStoreManagerImpl
    implements YumStoreManager
{

  private final Provider<YumStore> yumStoreProvider;

  private final Map<String, YumStore> stores;

  @Inject
  public YumStoreManagerImpl(final Provider<YumStore> yumStoreProvider) {
    this.yumStoreProvider = yumStoreProvider;
    stores = Maps.newHashMap();
  }

  @Override
  public YumStore add(final String repositoryId) {
    YumStore yumStore = yumStoreProvider.get();
    stores.put(repositoryId, yumStore);
    return yumStore;
  }

  @Override
  public void delete(final String repositoryId) {
    stores.remove(repositoryId);
  }

  @Override
  public YumStore get(String repositoryId) {
    return stores.get(repositoryId);
  }

}
