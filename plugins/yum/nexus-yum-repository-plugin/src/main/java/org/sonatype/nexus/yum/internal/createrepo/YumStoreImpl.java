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

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Maps;

/**
 * @since 3.0
 */
@Named
public class YumStoreImpl
    extends ComponentSupport
    implements YumStore
{

  private final Map<String, YumPackage> yumPackages;

  @Inject
  public YumStoreImpl() {
    yumPackages = Maps.newHashMap();
  }

  @Override
  public void add(YumPackage yumPackage) {
    yumPackages.put(yumPackage.getLocation(), yumPackage);
  }

  @Override
  public Iterable<YumPackage> get() {
    return yumPackages.values();
  }

  @Override
  public void delete(final String location) {
    log.debug("Remove {}", location);
    Iterator<String> it = yumPackages.keySet().iterator();
    while (it.hasNext()) {
      String key = it.next();
      if ((key + "/").startsWith(location)) {
        it.remove();
      }
    }
  }

}
