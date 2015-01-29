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

import com.google.common.io.CountingOutputStream
import javanet.staxutils.IndentingXMLStreamWriter
import org.sonatype.nexus.util.DigesterUtils

import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream

/**
 * @since 3.0
 */
class YumRepositoryWriter
implements Closeable
{

  private Output po

  private Output fo

  private Output oo

  XMLStreamWriter pw

  XMLStreamWriter fw

  XMLStreamWriter ow

  XMLStreamWriter rw

  private boolean open
  private boolean closed

  YumRepositoryWriter(final File repoDir) {
    XMLOutputFactory factory = XMLOutputFactory.newInstance()
    po = new Output(new FileOutputStream(new File(repoDir, 'primary.xml.gz')))
    pw = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(po.stream, "UTF-8"))

    fo = new Output(new FileOutputStream(new File(repoDir, 'filelists.xml.gz')))
    fw = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(fo.stream, "UTF-8"))

    oo = new Output(new FileOutputStream(new File(repoDir, 'other.xml.gz')))
    ow = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(oo.stream, "UTF-8"))

    rw = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(new FileOutputStream(new File(repoDir, 'repomd.xml')), "UTF-8"))
  }

  void writePrimary(final YumPackage yumPackage) {
    pw.writeStartElement('package')
    pw.writeAttribute('type', 'rpm')
    writeBase(yumPackage)
    writeFormat(yumPackage)
    pw.writeEndElement()
  }

  void writeFiles(final YumPackage yumPackage) {
    fw.writeStartElement('package')
    fw.writeAttribute('pkgid', yumPackage.pkgid)
    fw.writeAttribute('name', yumPackage.name)
    fw.writeAttribute('arch', yumPackage.arch)
    writeEl(fw, 'version', null, ['epoch': yumPackage.epoch, 'ver': yumPackage.version, 'rel': yumPackage.release])
    writeFileEntries(fw, yumPackage, false)
    fw.writeEndElement()
  }

  void writeOther(final YumPackage yumPackage) {
    ow.writeStartElement('package')
    ow.writeAttribute('pkgid', yumPackage.pkgid)
    ow.writeAttribute('name', yumPackage.name)
    ow.writeAttribute('arch', yumPackage.arch)
    yumPackage.changes.each { changelog ->
      writeEl(ow, 'changelog', changelog.text, ['author': changelog.author, 'date': changelog.date])
    }
    ow.writeEndElement()
  }

  private void writeBase(final YumPackage yumPackage) {
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
    writeEl(pw, 'location', null, ['href': yumPackage.location])
  }

  private void writeFormat(final YumPackage yumPackage) {
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
    writeFileEntries(pw, yumPackage, true)
    pw.writeEndElement()
  }

  private void writePCO(final List<YumPackage.Entry> entries, final String type) {
    if (entries) {
      pw.writeStartElement('rpm:' + type)
      entries.each { entry ->
        writeEl(pw, 'rpm:entry', null, [
            'name': entry.name,
            'flags': entry.flags,
            'epoch': entry.epoch, 'ver': entry.version, 'rel': entry.release,
            'pre': entry.pre ? '1' : null
        ])
      }
      pw.writeEndElement()
    }
  }

  private void writeFileEntries(XMLStreamWriter writer, final YumPackage yumPackage, final boolean primary) {
    def files = yumPackage.files
    if (files) {
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
  }

  void writeEl(XMLStreamWriter writer, final String name, final Object text, final Map<String, Object> attrib) {
    writer.writeStartElement(name)
    attrib?.each { key, value ->
      if (value) {
        writer.writeAttribute(key, value?.toString())
      }
    }
    if (text) {
      writer.writeCharacters(text.toString())
    }
    writer.writeEndElement()
  }

  void writeEl(XMLStreamWriter writer, final String name, final Object text) {
    writeEl(writer, name, text, null)
  }

  private void writeData(final Output output, final String type, final int timestamp) {
    rw.writeStartElement('data')
    rw.writeAttribute('type', type)
    writeEl(rw, 'checksum', output.compressedChecksum, ['type': 'sha256'])
    writeEl(rw, 'open-checksum', output.openChecksum, ['type': 'sha256'])
    writeEl(rw, 'location', null, ['href': "repodata/${type}.xml.gz"])
    writeEl(rw, 'timestamp', timestamp)
    writeEl(rw, 'size', output.compressedSize)
    writeEl(rw, 'open-size', output.openSize)
    rw.writeEndElement()
  }

  void maybeStart() {
    if (!open) {
      open = true

      pw.writeStartDocument('UTF-8', '1.0')
      pw.writeStartElement('metadata')
      pw.writeAttribute('xmlns', 'http://linux.duke.edu/metadata/common')
      pw.writeAttribute('xmlns:rpm', 'http://linux.duke.edu/metadata/rpm')

      fw.writeStartDocument('UTF-8', '1.0')
      fw.writeStartElement('filelists')
      fw.writeAttribute('xmlns', 'http://linux.duke.edu/metadata/filelists')

      ow.writeStartDocument('UTF-8', '1.0')
      ow.writeStartElement('otherdata')
      ow.writeAttribute('xmlns', 'http://linux.duke.edu/metadata/other')
    }
  }

  @Override
  void close() {
    maybeStart()

    assert !closed
    closed = true

    int timestamp = System.currentTimeMillis() / 1000

    pw.writeEndDocument()
    pw.close()
    po.stream.close()

    fw.writeEndDocument()
    fw.close()
    fo.stream.close()

    ow.writeEndDocument()
    ow.close()
    oo.stream.close()

    rw.writeStartDocument('UTF-8', '1.0')
    rw.writeStartElement('repomd')
    rw.writeAttribute('xmlns', 'http://linux.duke.edu/metadata/repo')
    rw.writeAttribute('xmlns:rpm', 'http://linux.duke.edu/metadata/rpm')
    writeData(po, 'primary', timestamp)
    writeData(fo, 'filelists', timestamp)
    writeData(oo, 'other', timestamp)
    rw.writeEndDocument()
    rw.close()
  }

  private static class Output
  {
    private CountingOutputStream openSizeStream
    private CountingOutputStream compressedSizeStream
    private DigestOutputStream openDigestStream
    private DigestOutputStream compressedDigestStream

    Output(final OutputStream stream) {
      compressedDigestStream = new DigestOutputStream(stream, MessageDigest.getInstance("SHA-256"))
      compressedSizeStream = new CountingOutputStream(compressedDigestStream)
      openDigestStream = new DigestOutputStream(new GZIPOutputStream(compressedSizeStream), MessageDigest.getInstance("SHA-256"))
      openSizeStream = new CountingOutputStream(openDigestStream)
    }

    OutputStream getStream() {
      return openSizeStream
    }

    long getOpenSize() {
      return openSizeStream.count
    }

    long getCompressedSize() {
      return compressedSizeStream.count
    }

    String getOpenChecksum() {
      return DigesterUtils.getDigestAsString(openDigestStream.messageDigest.digest())
    }

    String getCompressedChecksum() {
      return DigesterUtils.getDigestAsString(compressedDigestStream.messageDigest.digest())
    }
  }

}
