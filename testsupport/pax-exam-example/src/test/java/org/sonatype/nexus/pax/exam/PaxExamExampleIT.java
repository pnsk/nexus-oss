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
package org.sonatype.nexus.pax.exam;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import javax.inject.Inject;
import javax.servlet.Filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.doNotModifyLogConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

@RunWith(PaxExam.class)
public class PaxExamExampleIT
{
  @Inject
  @org.ops4j.pax.exam.util.Filter(value = "(name=nexus)", timeout = 60000)
  private Filter filter; // this service only appears once nexus is fully booted

  @Configuration
  public Option[] config() {
    return new Option[] { //

    vmOptions("-Xmx400m", "-XX:MaxPermSize=192m"), // taken from testsuite config

        karafDistributionConfiguration() //
            .karafVersion("3") //

            .frameworkUrl(maven() // our custom karaf distribution
                .groupId("org.sonatype.nexus.assemblies") //
                .artifactId("nexus-base-template") //
                .versionAsInProject() //
                .type("zip")) //

            .unpackDirectory(new File("target/nexus")) //

            .useDeployFolder(false), // prefer features over hot-deploy when installing extra bundles

        editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", // so pax-exam can fetch its feature
            "org.ops4j.pax.url.mvn.repositories", "https://repo1.maven.org/maven2@id=central"), //

        editConfigurationFilePut("etc/nexus.properties", // allocated random port as usual
            "application-port", Integer.toString(findFreePort())), //

        configureConsole().ignoreLocalConsole(), // no need for console

        doNotModifyLogConfiguration(), // don't mess with our logging

        keepRuntimeFolder(), // keep files around in case we need to debug
    };
  }

  @Test
  public void testNexusBoot() {

    // TODO: check status...
  }

  // should find a home in goodies?
  private static int findFreePort() {
    ServerSocket server;
    try {
      server = new ServerSocket(0);
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to allocate port", e);
    }
    int portNumber = server.getLocalPort();
    try {
      server.close();
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to release port " + portNumber, e);
    }
    return portNumber;
  }
}