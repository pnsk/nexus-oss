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
package org.sonatype.nexus.repository.view.matchers.token;

import java.util.Map;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link TokenParser}.
 */
public class TokenParserTest
  extends TestSupport
{
  @Test
  public void simplePattern() {
    String pattern = "/{a}/{b}/{c}";

    final TokenParser parser = new TokenParser(pattern);
    log(parser);

    final Map<String, String> tokens = parser.parse("/yes/no/false");

    assertThat(tokens, is(notNullValue()));
    assertThat(tokens.entrySet(), hasSize(3));

    assertThat(tokens.get("a"), is(equalTo("yes")));
    assertThat(tokens.get("b"), is(equalTo("no")));
    assertThat(tokens.get("c"), is(equalTo("false")));
  }

  @Test
  public void mavenLikeTemplate() {
    final String pattern = "/{group}/{module}/{version}/{name}-{version}.{ext}";
    final TokenParser parser = new TokenParser(pattern);
    log(parser);

    final Map<String, String> tokens = parser.parse("/foo/bar/1234/bar-1234.zip");

    assertThat(tokens, is(notNullValue()));
    assertThat(tokens.entrySet(), hasSize(5));

    assertThat(tokens.get("group"), is(equalTo("foo")));
    assertThat(tokens.get("module"), is(equalTo("bar")));
    assertThat(tokens.get("version"), is(equalTo("1234")));
    assertThat(tokens.get("name"), is(equalTo("bar")));
    assertThat(tokens.get("ext"), is(equalTo("zip")));
  }

  @Test
  public void mavenGroupsHaveMultiplePathSegments() {
    final String pattern = "/{group:.+}/{module}/{version}/{name}-{version}.{ext}";
    final TokenParser parser = new TokenParser(pattern);
    log(parser);

    final Map<String, String> tokens = parser.parse("/org/sonatype/nexus/components/1234/bar-1234.zip");

    assertThat(tokens, is(notNullValue()));
    assertThat(tokens.entrySet(), hasSize(5));

    assertThat(tokens.get("group"), is(equalTo("org/sonatype/nexus")));
    assertThat(tokens.get("module"), is(equalTo("components")));
    assertThat(tokens.get("version"), is(equalTo("1234")));
    assertThat(tokens.get("name"), is(equalTo("bar")));
    assertThat(tokens.get("ext"), is(equalTo("zip")));
  }

  @Test
  public void testNugetOperations() {
    final String pattern = "/{operation}({paramString:.*})";
    final TokenParser parser = new TokenParser(pattern);
    log(parser);

    final Map<String, String> tokens = parser.parse("/Packages()");

    assertThat(tokens, is(notNullValue()));
    assertThat(tokens.entrySet(), hasSize(2));

    assertThat(tokens.get("operation"), is(equalTo("Packages")));
    assertThat(tokens.get("paramString"), is(equalTo("")));
  }

  @Test
  public void slashesInTheSecondGroup() {
    final String pattern = "/{singleSegment}/{manySegments:.+}";
    final TokenParser parser = new TokenParser(pattern);
    log(parser);

    final Map<String, String> tokens = parser.parse("/1/2/3/4/5");

    assertThat(tokens, is(notNullValue()));
    assertThat(tokens.entrySet(), hasSize(2));

    assertThat(tokens.get("singleSegment"), is(equalTo("1")));
    assertThat(tokens.get("manySegments"), is(equalTo("2/3/4/5")));
  }
}
