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

import org.redline_rpm.ReadableChannelWrapper
import org.redline_rpm.header.AbstractHeader
import org.redline_rpm.header.AbstractHeader.Tag
import org.redline_rpm.header.Flags
import org.redline_rpm.header.Format
import org.redline_rpm.header.Header
import org.redline_rpm.header.Signature
import org.sonatype.nexus.util.DigesterUtils

import java.nio.channels.Channels

import static org.redline_rpm.ChannelWrapper.Key
import static org.redline_rpm.header.Header.HeaderTag

/**
 * @since 3.0
 */
class YumPackageParser
{

  YumPackage parse(final File rpm) {
    String checksum
    new FileInputStream(rpm).withStream { InputStream rpmStream ->
      checksum = DigesterUtils.getDigest('SHA-256', rpmStream)
    }
    new FileInputStream(rpm).withStream { InputStream rpmStream ->
      ReadableChannelWrapper rcw = new ReadableChannelWrapper(Channels.newChannel(rpmStream))
      rcw.withCloseable {
        Format format = read(rcw)
        //println format
        Signature signature = format.signature
        Header header = format.header
        return new YumPackage(
            checksum: checksum,
            checksum_type: 'sha256',
            name: asString(header, HeaderTag.NAME),
            arch: asString(header, HeaderTag.ARCH),
            version: asString(header, HeaderTag.VERSION),
            epoch: asString(header, HeaderTag.EPOCH),
            release: asString(header, HeaderTag.RELEASE),
            summary: asString(header, HeaderTag.SUMMARY),
            description: asString(header, HeaderTag.DESCRIPTION),
            url: asString(header, HeaderTag.URL),
            time_file: rpm.lastModified() / 1000,
            time_build: asInt(header, HeaderTag.BUILDTIME),
            rpm_license: asString(header, HeaderTag.LICENSE),
            rpm_vendor: asString(header, HeaderTag.VENDOR),
            rpm_group: asString(header, HeaderTag.GROUP),
            rpm_buildhost: asString(header, HeaderTag.BUILDHOST),
            rpm_sourcerpm: asString(header, HeaderTag.SOURCERPM),
            rpm_header_start: signature.getEndPos() + header.getStartPos(),
            rpm_header_end: header.getEndPos(),
            rpm_packager: asString(header, HeaderTag.PACKAGER),
            size_package: rpm.length(),
            size_installed: asInt(header, HeaderTag.SIZE),
            size_archive: asInt(signature, Signature.SignatureTag.PAYLOADSIZE),
            provides: parseDeps(header, HeaderTag.PROVIDENAME, HeaderTag.PROVIDEVERSION, HeaderTag.PROVIDEFLAGS),
            requires: parseDeps(header, HeaderTag.REQUIRENAME, HeaderTag.REQUIREVERSION, HeaderTag.REQUIREFLAGS),
            conflicts: parseDeps(header, HeaderTag.CONFLICTNAME, HeaderTag.CONFLICTVERSION, HeaderTag.CONFLICTFLAGS),
            obsoletes: parseDeps(header, HeaderTag.OBSOLETENAME, HeaderTag.OBSOLETEVERSION, HeaderTag.OBSOLETEFLAGS)
        )
      }
    }
  }

  def parseDeps(final Header header, final HeaderTag namesTag, final HeaderTag versionTag, final HeaderTag flagsTag) {
    def provides = []
    def names = header.getEntry(namesTag)?.values
    if (!names) {
      return null
    }
    def versions = header.getEntry(versionTag)?.values
    def flags = header.getEntry(flagsTag)?.values
    names.eachWithIndex { name, i ->
      def epoch = null, version = null, release = null, flag = null
      if (versions) {
        (epoch, version, release) = parseVersion(versions[i])
      }
      if (flags) {
        flag = flags[i]
      }
      provides << new YumPackage.Entry(
          name: name,
          epoch: epoch,
          version: version,
          release: release,
          flags: flag,
          pre: flag & (Flags.PREREQ | Flags.SCRIPT_PRE | Flags.SCRIPT_POST)
      )
    }
    return provides
  }

  def parseVersion(final String fullVersion) {
    def epoch = null, version = null, release = null
    if (fullVersion) {
      def i = fullVersion.indexOf(':')
      if (i != -1) {
        try {
          epoch = String.valueOf(fullVersion.substring(0, i))
        }
        catch (NumberFormatException e) {
          epoch = '0'
        }
      }
      def j = fullVersion.indexOf('-')
      if (j != -1) {
        release = fullVersion.substring(j + 1)
        version = fullVersion.substring(Math.max(i, 0), j)
      }
      else {
        version = fullVersion.substring(Math.max(i, 0))
      }
    }
    return [epoch, version, release]
  }

  def read(ReadableChannelWrapper content) throws IOException {
    Format format = new Format()

    Key<Integer> headerStartKey = content.start()

    Key<Integer> leadStartKey = content.start()
    format.lead.read(content)
    content.finish(leadStartKey)

    Key<Integer> signatureStartKey = content.start()
    format.signature.read(content)
    content.finish(signatureStartKey)

    Integer headerStartPos = content.finish(headerStartKey)
    format.header.setStartPos(headerStartPos)
    Key<Integer> headerKey = content.start()
    format.header.read(content)
    Integer headerLength = content.finish(headerKey)
    format.header.setEndPos(headerStartPos + headerLength)

    return format
  }

  private String asString(final AbstractHeader header, final Tag tag) {
    AbstractHeader.Entry<?> entry = header.getEntry(tag)
    if (!entry) {
      return null
    }
    Object values = entry.values
    return values[0]
  }

  private int asInt(final AbstractHeader header, final Tag tag) {
    AbstractHeader.Entry<?> entry = header.getEntry(tag)
    if (!entry) {
      return 0
    }
    Object values = entry.values
    return values[0]
  }

}
