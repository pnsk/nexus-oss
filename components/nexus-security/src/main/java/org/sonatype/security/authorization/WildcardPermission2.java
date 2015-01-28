package org.sonatype.security.authorization;

import org.apache.shiro.authz.permission.WildcardPermission;

/**
 * {@link WildcardPermission} which caches {@link #hashCode} for improved performance.
 *
 * @since 3.0
 */
public class WildcardPermission2
  extends WildcardPermission
{
  private final int cachedHash;

  public WildcardPermission2(final String wildcardString) {
    super(wildcardString);
    this.cachedHash = super.hashCode();
  }

  @Override
  public int hashCode() {
    return cachedHash;
  }
}
