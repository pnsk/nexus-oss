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
/*global Ext, NX*/

/**
 * Settings FieldSet.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.formfield.SettingsFieldSet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-formfield-settingsfieldset',

  requires: [
    'NX.coreui.view.formfield.factory.FormfieldCheckboxFactory',
    'NX.coreui.view.formfield.factory.FormfieldComboFactory',
    'NX.coreui.view.formfield.factory.FormfieldDateFieldFactory',
    'NX.coreui.view.formfield.factory.FormfieldNumberFieldFactory',
    'NX.coreui.view.formfield.factory.FormfieldTextAreaFactory',
    'NX.coreui.view.formfield.factory.FormfieldTextFieldFactory'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      autoHeight: false,
      autoScroll: true,
      collapsed: false,
      items: []
    });

    me.callParent(arguments);
  },

  /**
   * @property
   */
  formFields: undefined,

  /**
   * Renders form fields.
   * @param {Array} formFields form fields to rendered
   */
  setFormFields: function (formFields) {
    var me = this,
        item;

    me.formFields = formFields;

    me.removeAll();

    if (me.formFields) {
      Ext.Array.each(me.formFields, function (formField) {
        var factory = Ext.ClassManager.getByAlias('nx.formfield.factory.' + formField.type);
        if (!factory) {
          factory = Ext.ClassManager.getByAlias('nx.formfield.factory.string');
        }
        if (factory) {
          item = Ext.apply(factory.create(formField), {
            requiresPermission: true,
            name: 'property_' + formField.id,
            factory: factory
          });
          me.add(item);
        }
      });
    }
    me.up('form').isValid();
  },

  /**
   * Exports properties.
   * @returns {Object} properties object
   */
  exportProperties: function (values) {
    var me = this,
        properties = {},
        value;

    if (me.formFields) {
      Ext.Array.each(me.formFields, function (formField) {
        value = values['property_' + formField.id];
        if (Ext.isDefined(value) && value !== null) {
          properties[formField.id] = String(value);
          delete values['property_' + formField.id];
        }
      });
    }

    return properties;
  },

  /**
   * Imports properties.
   * @param {Object} properties to import
   * @param {Array} formFields to import
   */
  importProperties: function (properties, formFields) {
    var me = this,
        form = me.up('form').getForm(),
        data = {};

    me.setFormFields(formFields);

    if (me.formFields) {
      Ext.Array.each(me.formFields, function (formField) {
        data['property_' + formField.id] = '';
      });
    }

    if (properties) {
      Ext.Object.each(properties, function (key, value) {
        data['property_' + key] = value;
      });
    }

    form.setValues(data);
  },

  /**
   * Mark fields in this form invalid in bulk.
   * @param {Object/Object[]/Ext.data.Errors} errors
   * Either an array in the form `[{id:'fieldId', msg:'The message'}, ...]`,
   * an object hash of `{id: msg, id2: msg2}`, or a {@link Ext.data.Errors} object.
   */
  markInvalid: function (errors) {
    var me = this,
        form = me.up('form').getForm(),
        remainingMessages = [],
        key, marked, field;

    if (Ext.isDefined(errors)) {
      for (key in errors) {
        if (errors.hasOwnProperty(key)) {
          marked = false;
          if (form) {
            field = form.findField('property_' + key);
            if (!field) {
              field = form.findField(key);
            }
            if (field) {
              marked = true;
              field.markInvalid(errors[key]);
            }
          }
          if (!marked) {
            remainingMessages.push(errors[key]);
          }
        }
      }
    }

    if (remainingMessages.length > 0) {
      NX.Messages.add({ text: remainingMessages.join('\n'), type: 'warning' });
    }
  }

});
