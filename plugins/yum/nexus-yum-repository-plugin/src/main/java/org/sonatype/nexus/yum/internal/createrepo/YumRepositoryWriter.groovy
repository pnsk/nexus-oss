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

import javanet.staxutils.IndentingXMLStreamWriter

import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

/**
 * @since 3.0
 */
class YumRepositoryWriter
implements Closeable
{

  private XMLStreamWriter pw

  private XMLStreamWriter fw

  private XMLStreamWriter ow

  private XMLStreamWriter rw

  private boolean open
  private boolean closed

  YumRepositoryWriter(final File outputDir) {
    XMLOutputFactory factory = XMLOutputFactory.newInstance()
    pw = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(new FileOutputStream(new File(outputDir, 'primary.xml')), "UTF8"))
    fw = factory.createXMLStreamWriter(new FileOutputStream(new File(outputDir, 'files.xml')), "UTF8")
    ow = factory.createXMLStreamWriter(new FileOutputStream(new File(outputDir, 'others.xml')), "UTF8")
    rw = factory.createXMLStreamWriter(new FileOutputStream(new File(outputDir, 'repomd.xml')), "UTF8")
  }

  void push(final YumPackage yumPackage) {
    maybeStart()
    writePrimary(yumPackage)
  }

  private def writePrimary(final YumPackage yumPackage) {
    pw.writeStartElement('package')
    pw.writeAttribute('type', 'rpm')
    writeBase(yumPackage)
    writeFormat(yumPackage)
    pw.writeEndElement()
  }

  private def writeBase(final YumPackage yumPackage) {
    writeEl(pw, 'name', yumPackage.name)
    writeEl(pw, 'arch', yumPackage.arch)
    writeEl(pw, 'version', null, ['epoch': yumPackage.epoch, 'ver': yumPackage.version, 'rel': yumPackage.release])
    writeEl(pw, 'checksum', yumPackage.checksum, ['type': yumPackage.checksum_type, 'pkgid': 'YES'])
    writeEl(pw, 'summary', yumPackage.summary)
    writeEl(pw, 'description', yumPackage.description)
    writeEl(pw, 'packager', yumPackage.rpm_packager)
    writeEl(pw, 'url', yumPackage.url)
    writeEl(pw, 'time', null, ['file': yumPackage.time_file, 'build': yumPackage.time_build])
    writeEl(pw, 'size', null, ['package': yumPackage.size_package, 'installed': yumPackage.size_installed, 'archive': yumPackage.size_archive])
    // TODO location el
  }

  private def writeFormat(final YumPackage yumPackage) {
    pw.writeStartElement('format')
    writeEl(pw, 'rpm:license', yumPackage.rpm_license)
    writeEl(pw, 'rpm:vendor', yumPackage.rpm_vendor)
    writeEl(pw, 'rpm:group', yumPackage.rpm_group)
    writeEl(pw, 'rpm:buildhost', yumPackage.rpm_buildhost)
    writeEl(pw, 'rpm:sourcerpm', yumPackage.rpm_sourcerpm)
    writeEl(pw, 'rpm:header-range', null, ['start': yumPackage.rpm_header_start, 'end': yumPackage.rpm_header_end])
    writePCO(yumPackage.provides, 'provides')
    writePCO(yumPackage.requires, 'requires')
    writePCO(yumPackage.conflicts, 'conflicts')
    writePCO(yumPackage.obsoletes, 'obsoletes')
    writeFiles(pw, yumPackage, true)
    pw.writeEndElement()
  }

  def writePCO(final List<YumPackage.Entry> entries, final String type) {
    if (entries) {
      pw.writeStartElement('rpm:' + type)
      entries.each { entry ->
        def flags = entry.flags & 0xf
        def flagsStr = null
        if (flags == 2) {
          flagsStr = 'LT'
        }
        else if (flags == 4) {
          flagsStr = 'GT'
        }
        else if (flags == 8) {
          flagsStr = 'EQ'
        }
        else if (flags == 10) {
          flagsStr = 'LE'
        }
        else if (flags == 12) {
          flagsStr = 'GE'
        }

        writeEl(pw, 'rpm:entry', null, ['name': entry.name, 'flags': flagsStr, 'epoch': entry.epoch, 'ver': entry.version, 'rel': entry.release])
      }
      pw.writeEndElement()
    }
  }

  private def writeFiles(XMLStreamWriter writer, final YumPackage yumPackage, final boolean primary) {
    def files = yumPackage.files
    if (primary) {
      files = files.findResults { YumPackage.File file -> file.primary ? file : null }
    }
    files.findResults { YumPackage.File file -> file.type == YumPackage.FileType.file ? file : null }.each { file ->
      writeEl(writer, 'file', file.name)
    }
    files.findResults { YumPackage.File file -> file.type == YumPackage.FileType.dir ? file : null }.each { file ->
      writeEl(writer, 'file', file.name, ['type': file.type])
    }
    files.findResults { YumPackage.File file -> file.type == YumPackage.FileType.ghost ? file : null }.each { file ->
      writeEl(writer, 'file', file.name, ['type': file.type])
    }
  }

  private def writeEl(XMLStreamWriter writer, final String name, final String text, final Map<String, Object> attrib) {
    writer.writeStartElement(name)
    attrib?.each { key, value ->
      if (value) {
        writer.writeAttribute(key, value?.toString())
      }
    }
    if (text) {
      writer.writeCharacters(text)
    }
    writer.writeEndElement()
  }

  private def writeEl(XMLStreamWriter writer, final String name, final String text) {
    writeEl(writer, name, text, null)
  }

  private def maybeStart() {
    if (!open) {
      open = true

      pw.writeStartDocument('UTF-8', '1.0')
      pw.writeStartElement('metadata')
      pw.writeAttribute('xmlns', 'http://linux.duke.edu/metadata/common')
      pw.writeAttribute('xmlns:rpm', 'http://linux.duke.edu/metadata/rpm')

      fw.writeStartDocument()
      ow.writeStartDocument()
    }
  }

  @Override
  void close() {
    maybeStart()

    assert !closed

    closed = true

    pw.writeEndDocument()

    fw.writeEndDocument()
    ow.writeEndDocument()

    pw.close()
    fw.close()
    ow.close()

    rw.writeStartDocument()
    rw.writeEndDocument()
    rw.close()
  }

}
