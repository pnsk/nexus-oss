/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global NX, Ext, Nexus, Sonatype*/

/**
 * Logging panel.
 *
 * @since 2.7
 */
NX.define('Nexus.logging.view.Panel', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: [
    'Nexus.logging.view.Loggers',
    'Nexus.logging.view.Log',
    'Nexus.logging.Icons',
  ],

  xtype: 'nx-logging-view-panel',

  title: 'Logging',

  border: false,
  layout: {
    type: 'vbox',
    align: 'stretch'
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.logging.Icons,
        sp = Sonatype.lib.Permissions,
        items = [
          {
            xtype: 'panel',
            border: false,
            cls: 'nx-logging-view-panel-description',
            html: icons.get('logging').variant('x32').img +
                '<div>Allows changing logging configuration and viewing the current log. ' +
                'For more information see the <a href="http://links.sonatype.com/products/nexus/oss/docs" target="_blank">book pages for logging configuration</a></div>',
            height: 60,
            flex: 0
          }
        ],
        tabs = [];

    if (sp.checkPermission('nexus:logconfig', sp.READ)) {
      tabs.push({ xtype: 'nx-logging-view-loggers' });
    }
    tabs.push({ xtype: 'nx-logging-view-log' });

    if (tabs.length > 0) {
      items.push({
        xtype: 'tabpanel',
        border: false,
        plain: true,
        layoutOnTabChange: true,
        flex: 1,
        items: tabs,
        activeItem: 0
      });
    }

    Ext.apply(me, {
      items: items
    });

    me.constructor.superclass.initComponent.apply(me, arguments);
  }
});