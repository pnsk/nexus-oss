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
package org.sonatype.nexus.proxy.item.uid;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.util.SystemPropertiesHelper;

/**
 * Attribute saying is UID covering a content deliverable remotely.
 *
 * @author cstamas
 */
public class IsRemotelyAccessibleAttribute
    implements Attribute<Boolean>
{
  // HACK: Allow this attribute function to be disabled for NEXUS-8060
  private static final boolean attributes = SystemPropertiesHelper.getBoolean(
      IsRemotelyAccessibleAttribute.class.getName() + ".attributes", false);

  // HACK: Allow this attribute function to be disabled for NEXUS-8060
  private static final boolean trash = SystemPropertiesHelper.getBoolean(
      IsRemotelyAccessibleAttribute.class.getName() + ".trash", false);

  public Boolean getValueFor(RepositoryItemUid subject) {
    return (attributes || !subject.getBooleanAttributeValue(IsItemAttributeMetacontentAttribute.class))
        && (trash || !subject.getBooleanAttributeValue(IsTrashMetacontentAttribute.class));
  }
}
