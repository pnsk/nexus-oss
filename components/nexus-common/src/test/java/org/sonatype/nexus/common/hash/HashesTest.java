package org.sonatype.nexus.common.hash;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;

/**
 * Tests for {@link Hashes}.
 */
public class HashesTest
{
  private static final String DATA = "This is a test message for hashing!";

  private static final String MD5_HASH = "b4b91fa27dd64d4f14cd1e22e6a3c714";
  private static final String SHA1_HASH = "410fee1895a6af9449ae1647276259fd69a75b15";
  private static final String SHA512_HASH = "b90de0708205534bf3bc4e478c3718c7bf78b5ec60902dbbea234aadd748c004cdf94deda2034b0fa8bdc559ac59d6ac622211956bf782da33444d29e8d9f160";

  @Test
  public void hashOne() throws Exception {
    HashCode hashCode = Hashes.hash(inputStream(), MD5);

    assertThat(hashCode.toString(), is(MD5_HASH));
  }

  @Test
  public void hashThree() throws Exception {
    Map<HashAlgorithm, HashCode> hashes = Hashes.hash(inputStream(), ImmutableList.of(MD5, SHA1, SHA512));

    assertThat(hashes.size(), is(3));
    assertThat(hashes.get(MD5).toString(), is(MD5_HASH));
    assertThat(hashes.get(SHA1).toString(), is(SHA1_HASH));
    assertThat(hashes.get(SHA512).toString(), is(SHA512_HASH));
  }

  @Test
  public void hashZero() throws Exception {
    List<HashAlgorithm> zeroAlgorithms = ImmutableList.of();
    Map<HashAlgorithm, HashCode> hashes = Hashes.hash(inputStream(), zeroAlgorithms);

    assertThat(hashes.size(), is(0));
  }

  private static InputStream inputStream() {
    return new ByteArrayInputStream(DATA.getBytes(Charsets.UTF_8));
  }
}
