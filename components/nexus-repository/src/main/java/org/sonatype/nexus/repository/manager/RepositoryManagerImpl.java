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

package org.sonatype.nexus.repository.manager;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.security.SecurityHelper;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.security.BreadActions.ADD;
import static org.sonatype.nexus.repository.security.BreadActions.BROWSE;
import static org.sonatype.nexus.repository.security.BreadActions.DELETE;
import static org.sonatype.nexus.repository.security.BreadActions.EDIT;
import static org.sonatype.nexus.repository.security.BreadActions.READ;
import static org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor.permission;

/**
 * Default {@link RepositoryManager} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RepositoryManagerImpl
    extends StateGuardLifecycleSupport
    implements RepositoryManager
{
  private final EventBus eventBus;

  private final SecurityHelper securityHelper;

  private final ConfigurationStore store;

  private final Map<String, Recipe> recipes;

  private final RepositoryFactory factory;

  private final Provider<ConfigurationFacet> configFacet;

  private final RepositoryAdminSecurityResource securityResource;

  private final Map<String, Repository> repositories = Maps.newHashMap();

  @Inject
  public RepositoryManagerImpl(final EventBus eventBus,
                               final SecurityHelper securityHelper,
                               final ConfigurationStore store,
                               final RepositoryFactory factory,
                               final Provider<ConfigurationFacet> configFacet,
                               final Map<String, Recipe> recipes,
                               final RepositoryAdminSecurityResource securityResource)
  {
    this.eventBus = checkNotNull(eventBus);
    this.securityHelper = checkNotNull(securityHelper);
    this.store = checkNotNull(store);
    this.factory = checkNotNull(factory);
    this.configFacet = checkNotNull(configFacet);
    this.recipes = checkNotNull(recipes);
    this.securityResource = checkNotNull(securityResource);
  }

  /**
   * Lookup a recipe by name.
   */
  private Recipe recipe(final String name) {
    Recipe recipe = recipes.get(name);
    checkState(recipe != null, "Missing recipe: %s", name);
    return recipe;
  }

  /**
   * Lookup a repository by name.
   */
  private Repository repository(final String name) {
    Repository repository = repositories.get(name);
    checkState(repository != null, "Missing repository: %s", name);
    return repository;
  }

  /**
   * Construct a new repository from configuration.
   */
  private Repository newRepository(final Configuration configuration) throws Exception {
    String recipeName = configuration.getRecipeName();
    Recipe recipe = recipe(recipeName);
    log.debug("Using recipe: [{}] {}", recipeName, recipe);

    Repository repository = factory.create(recipe.getType(), recipe.getFormat());
    repository.init(configuration);

    // attach mandatory facets
    repository.attach(configFacet.get());

    // apply recipe to repository
    recipe.apply(repository);

    // verify required facets
    repository.facet(ViewFacet.class);

    return repository;
  }

  /**
   * Track repository.
   */
  private void track(final Repository repository) {
    log.debug("Tracking: {}", repository);
    repositories.put(repository.getName(), repository);
  }

  /**
   * Untrack repository.
   */
  private void untrack(final Repository repository) {
    log.debug("Untracking: {}", repository);
    repositories.remove(repository.getName());
  }

  // TODO: Generally need to consider exception handling to ensure proper state is maintained always

  @Override
  protected void doStart() throws Exception {
    List<Configuration> configurations = store.list();
    if (configurations.isEmpty()) {
      log.debug("No repositories configured");
      return;
    }

    log.debug("Restoring {} repositories", configurations.size());
    for (Configuration configuration : configurations) {
      log.debug("Restoring repository: {}", configuration);
      Repository repository = newRepository(configuration);
      securityResource.add(repository);
      track(repository);

      eventBus.post(new RepositoryLoadedEvent(repository));
    }

    log.debug("Starting {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      log.debug("Starting repository: {}", repository);
      repository.start();

      eventBus.post(new RepositoryRestoredEvent(repository));
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (repositories.isEmpty()) {
      log.debug("No repositories defined");
      return;
    }

    log.debug("Stopping {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      log.debug("Stopping repository: {}", repository);
      repository.stop();
    }

    log.debug("Destroying {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      log.debug("Destroying repository: {}", repository);
      repository.destroy();
    }

    repositories.clear();
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<Repository> browse() {
    // lookup subject to avoid re-resolving this for each item in the list
    final Subject subject = securityHelper.subject();
    return Iterables.filter(repositories.values(), new Predicate<Repository>()
    {
      @Override
      public boolean apply(final Repository input) {
        return securityHelper.allPermitted(subject, permission(input.getName(), BROWSE));
      }
    });
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Repository get(final String name) {
    checkNotNull(name);

    securityHelper.ensurePermitted(permission(name, READ));

    return repositories.get(name);
  }

  @Override
  @Guarded(by = STARTED)
  public Repository create(final Configuration configuration) throws Exception {
    checkNotNull(configuration);
    checkNotNull(configuration.getRepositoryName());

    log.debug("Creating repository: {}", configuration);

    securityHelper.ensurePermitted(permission(configuration.getRepositoryName(), ADD));
    store.create(configuration);
    Repository repository = newRepository(configuration);
    securityResource.add(repository);
    track(repository);

    repository.start();

    eventBus.post(new RepositoryCreatedEvent(repository));

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public Repository update(final Configuration configuration) throws Exception {
    checkNotNull(configuration);
    checkNotNull(configuration.getRepositoryName());

    log.debug("Updating repository: {}", configuration);

    securityHelper.ensurePermitted(permission(configuration.getRepositoryName(), EDIT));
    Repository repository = repository(configuration.getRepositoryName());

    // TODO: Ensure configuration sanity, before we apply to repository
    repository.stop();
    repository.update(configuration);
    repository.start();

    eventBus.post(new RepositoryUpdatedEvent(repository));

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final String name) throws Exception {
    checkNotNull(name);

    log.debug("Deleting repository: {}", name);

    securityHelper.ensurePermitted(permission(name, DELETE));
    Repository repository = repository(name);
    Configuration configuration = repository.getConfiguration();
    repository.stop();
    repository.delete();
    repository.destroy();
    store.delete(configuration);
    securityResource.remove(repository);
    untrack(repository);

    eventBus.post(new RepositoryDeletedEvent(repository));
  }
}
