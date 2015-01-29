/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.yum.internal.createrepo

import org.sonatype.nexus.yum.internal.RepoMD

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import java.util.zip.GZIPInputStream

/**
 * @since 3.0
 */
class YumRepositoryReader
{

  YumStore parse(final File repoDir) {
    XMLStreamReader pr = null
    XMLStreamReader fr = null
    XMLStreamReader or = null

    try {
      XMLInputFactory factory = XMLInputFactory.newInstance()
      new FileInputStream(new File(repoDir, "repodata/repomd.xml")).withStream { InputStream repoMDIn ->
        RepoMD repoMD = new RepoMD(repoMDIn)
        pr = factory.createXMLStreamReader(new GZIPInputStream(new FileInputStream(new File(repoDir, repoMD.getLocation('primary')))), "UTF-8")
        fr = factory.createXMLStreamReader(new GZIPInputStream(new FileInputStream(new File(repoDir, repoMD.getLocation('files')))), "UTF-8")
        or = factory.createXMLStreamReader(new GZIPInputStream(new FileInputStream(new File(repoDir, repoMD.getLocation('other')))), "UTF-8")
      }

      YumStoreImpl yumStore = new YumStoreImpl()
      return yumStore
    }
    finally {
      pr?.close()
      fr?.close()
      or?.close()
    }
  }

}
