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
package org.sonatype.nexus.rest.authentication;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.rest.model.AliasingListConverter;
import org.sonatype.nexus.rest.model.HtmlUnescapeStringConverter;
import org.sonatype.plexus.rest.ReferenceFactory;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.rest.model.PlexusRoleResource;
import org.sonatype.security.rest.model.PlexusUserResource;
import org.sonatype.security.rest.model.RoleAndPrivilegeListFilterResource;
import org.sonatype.security.rest.model.RoleAndPrivilegeListResource;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.UserChangePasswordResource;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserStatus;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import org.apache.commons.lang.StringEscapeUtils;
import org.restlet.data.Reference;
import org.restlet.data.Request;

/**
 * Base class of SecurityPlexusResources. Contains error handling util methods and conversion between DTO and
 * persistence model.
 *
 * @author bdemers
 */
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
public abstract class AbstractSecurityPlexusResource
    extends AbstractPlexusResource
{

  @Inject
  private SecuritySystem securitySystem;

  protected static final String DEFAULT_SOURCE = "default";

  @Inject
  protected ReferenceFactory referenceFactory;

  protected SecuritySystem getSecuritySystem() {
    return securitySystem;
  }

  protected ErrorResponse getErrorResponse(String id, String msg) {
    ErrorResponse ner = new ErrorResponse();
    ErrorMessage ne = new ErrorMessage();
    ne.setId(id);
    ne.setMsg(msg);
    ner.addError(ne);
    return ner;
  }


  protected PlexusUserResource securityToRestModel(User user) {
    PlexusUserResource resource = new PlexusUserResource();

    resource.setUserId(user.getUserId());
    resource.setSource(user.getSource());
    resource.setFirstName(user.getFirstName());
    resource.setLastName(user.getLastName());
    resource.setEmail(user.getEmailAddress());
    resource.setStatus(user.getStatus().name());

    for (RoleIdentifier role : user.getRoles()) {
      resource.addRole(this.securityToRestModel(role));
    }

    return resource;
  }

  // TODO: come back to this, we need to change the PlexusRoleResource
  protected PlexusRoleResource securityToRestModel(RoleIdentifier role) {
    // TODO: We shouldn't be looking up the role name here anyway... this should get pushed up to the
    // SecuritySystem.
    String roleName = role.getRoleId();

    SecuritySystem securitySystem = this.getSecuritySystem();

    try {
      AuthorizationManager authzManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE);
      roleName = authzManager.getRole(role.getRoleId()).getName();
    }
    catch (NoSuchAuthorizationManagerException e) {
      this.getLogger().warn("Failed to lookup the users Role: " + role.getRoleId() + " source: "
          + role.getSource() + " but the user has this role.", e);
    }
    catch (NoSuchRoleException e) {
      // this is a Warning if the role's source is default, if its not, then we most of the time it would not be
      // found anyway.
      if (DEFAULT_SOURCE.equals(role.getSource())) {
        this.getLogger().warn("Failed to lookup the users Role: " + role.getRoleId() + " source: "
            + role.getSource() + " but the user has this role.", e);
      }
      else {
        this.getLogger().debug("Failed to lookup the users Role: " + role.getRoleId() + " source: "
            + role.getSource() + " falling back to the roleId for the role's name.");
      }
    }

    PlexusRoleResource roleResource = new PlexusRoleResource();
    roleResource.setRoleId(role.getRoleId());
    roleResource.setName(roleName);
    roleResource.setSource(role.getSource());

    return roleResource;
  }

  protected Reference createChildReference(Request request, String childPath) {
    return this.referenceFactory.createChildReference(request, childPath);
  }

  protected void checkUsersStatus(String status)
      throws InvalidConfigurationException
  {
    boolean found = false;
    for (UserStatus userStatus : UserStatus.values()) {
      if (userStatus.name().equals(status)) {
        found = true;
      }
    }

    if (!found) {
      ValidationResponse response = new ValidationResponse();
      response.addValidationError(new ValidationMessage("status", "Users status is not valid."));
      throw new InvalidConfigurationException(response);
    }
  }

  protected String getRequestAttribute(final Request request, final String key, final boolean decode) {
    final String value = request.getAttributes().get(key).toString();

    if (decode) {
      try {
        return URLDecoder.decode(value, "UTF-8");
      }
      catch (UnsupportedEncodingException e) {
        getLogger().warn("Failed to decode URL attribute.", e);
      }
    }

    return value;
  }

  @Override
  public void configureXStream(final XStream xstream) {
    super.configureXStream(xstream);
    xstream.registerLocalConverter(UserChangePasswordResource.class, "oldPassword",
        new HtmlUnescapeStringConverter(true));
    xstream
        .registerLocalConverter(UserChangePasswordResource.class, "newPassword", new HtmlUnescapeStringConverter(true));
    xstream.registerLocalConverter(RoleResource.class, "id", new HtmlUnescapeStringConverter(true));
    xstream.registerLocalConverter(UserResource.class, "userId", new HtmlUnescapeStringConverter(true));
    xstream.registerLocalConverter(UserResource.class, "password", new HtmlUnescapeStringConverter(true));
    xstream.registerLocalConverter(RoleAndPrivilegeListResource.class, "id", new HtmlUnescapeStringConverter(true));

    xstream.registerLocalConverter(UserResource.class, "roles", new HtmlUnescapeStringCollectionConverter("role"));
    xstream.registerLocalConverter(RoleResource.class, "roles", new HtmlUnescapeStringCollectionConverter("role"));
    xstream.registerLocalConverter(RoleResource.class, "privileges",
        new HtmlUnescapeStringCollectionConverter("privilege"));
    xstream.registerLocalConverter(RoleAndPrivilegeListFilterResource.class, "selectedRoleIds",
        new HtmlUnescapeStringCollectionConverter("selectedRoleId"));
    xstream.registerLocalConverter(RoleAndPrivilegeListFilterResource.class, "selectedPrivilegeIds",
        new HtmlUnescapeStringCollectionConverter("selectedPrivilegeId"));
    xstream.registerLocalConverter(RoleAndPrivilegeListFilterResource.class, "hiddenRoleIds",
        new HtmlUnescapeStringCollectionConverter("hiddenRoleId"));
    xstream.registerLocalConverter(RoleAndPrivilegeListFilterResource.class, "hiddenPrivilegeIds",
        new HtmlUnescapeStringCollectionConverter("hiddenPrivilegeId"));
  }

  private static class HtmlUnescapeStringCollectionConverter
      extends AliasingListConverter
  {

    public HtmlUnescapeStringCollectionConverter(String alias) {
      super(String.class, alias);
    }

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
      final List<Object> unmarshal = (List<Object>) super.unmarshal(reader, context);

      // return value needs to be a "real" List
      return Lists.newArrayList(Collections2.transform(unmarshal, new Function()
      {
        @Nullable
        @Override
        public Object apply(@Nullable final Object input) {
          if (input instanceof String) {
            return StringEscapeUtils.unescapeHtml((String) input);
          }

          return input;
        }
      }));
    }
  }
}
