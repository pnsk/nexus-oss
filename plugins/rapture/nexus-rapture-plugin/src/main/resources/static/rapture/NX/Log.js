/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Global logging helper.
 *
 * @since 3.0
 */
Ext.define('NX.Log', {
  singleton: true,

  /**
   * @param {String} level
   * @param {Array} args
   */
  log: function (level, args) {
    var config = {
      level: level,
      msg: args.join(' ')
    };

    // translate debug -> log for Ext.log
    if (level === 'debug') {
      config.level = 'log';
    }

    Ext.log(config);
  },

  /**
   * @public
   */
  debug: function () {
    this.log('debug', Array.prototype.slice.call(arguments));
  },

  /**
   * @public
   */
  info: function () {
    this.log('info', Array.prototype.slice.call(arguments));
  },

  /**
   * @public
   */
  warn: function () {
    this.log('warn', Array.prototype.slice.call(arguments));
  },

  /**
   * @public
   */
  error: function () {
    this.log('error', Array.prototype.slice.call(arguments));
  }
});