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
package org.sonatype.nexus.repository.raw.internal;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.raw.internal.negativecache.NegativeCacheHandler;
import org.sonatype.nexus.repository.raw.internal.negativecache.NegativeCacheImpl;
import org.sonatype.nexus.repository.raw.internal.proxy.ProxyFacetImpl;
import org.sonatype.nexus.repository.raw.internal.proxy.ProxyHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.StorageFacetImpl;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.AlwaysMatcher;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

/**
 * A recipe for creating 'proxy' repositories for the 'raw' format.
 *
 * @since 3.0
 */
@Named("raw-proxy")
@Singleton
public class RawProxyRecipe
    extends RecipeSupport
{
  private final Provider<RawSecurityFacet> securityFacet;

  private final NegativeCacheHandler negativeCacheHandler;

  private final ProxyHandler proxyHandler;

  private final TimingHandler timingHandler;

  private final SecurityHandler securityHandler;

  private final Provider<ConfigurableViewFacet> viewFacetProvider;

  // Facets that don't require configuration
  private final List<Provider<? extends Facet>> facetProviders = Lists.newArrayList();

  @Inject
  public RawProxyRecipe(final @Named("proxy") Type type,
                        final @Named("raw") Format format,
                        final NegativeCacheHandler negativeCacheHandler,
                        final ProxyHandler proxyHandler,
                        final TimingHandler timingHandler,
                        final SecurityHandler securityHandler,

                        final Provider<RawSecurityFacet> securityFacet,

                        final Provider<ConfigurableViewFacet> viewFacetProvider,

                        final Provider<NegativeCacheImpl> negativeCache,

                        final Provider<ProxyFacetImpl> proxyFacet,
                        final Provider<HttpClientFacet> httpClient,
                        final Provider<RawPayloadStorage> payloadStorageFacet,

                        final Provider<RawStorageFacetImpl> rawStorageFacet,
                        final Provider<StorageFacetImpl> storageFacet)
  {
    super(type, format);

    this.securityFacet = checkNotNull(securityFacet);
    this.negativeCacheHandler = checkNotNull(negativeCacheHandler);
    this.proxyHandler = checkNotNull(proxyHandler);
    this.timingHandler = checkNotNull(timingHandler);
    this.securityHandler = checkNotNull(securityHandler);

    this.viewFacetProvider = checkNotNull(viewFacetProvider);

    facetProviders.add(checkNotNull(httpClient));
    facetProviders.add(checkNotNull(negativeCache));
    facetProviders.add(checkNotNull(proxyFacet));
    facetProviders.add(checkNotNull(payloadStorageFacet));
    facetProviders.add(checkNotNull(rawStorageFacet));
    facetProviders.add(checkNotNull(storageFacet));
  }

  @Override
  public void apply(final @Nonnull Repository repository) throws Exception {
    repository.attach(securityFacet.get());

    repository.attach(configure(viewFacetProvider.get()));

    for (Provider<? extends Facet> facetProvider : facetProviders) {
      repository.attach(facetProvider.get());
    }
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder router = new Router.Builder();

    // Build the primary route of the raw proxy view

    router.route(new Route.Builder()
            .matcher(new AlwaysMatcher())
            .handler(timingHandler)
            .handler(timingHandler)
            .handler(negativeCacheHandler)
            .handler(proxyHandler)
            .handler(notFound())
            .create()
    );

    // By default, return a 404
    router.defaultHandlers(notFound());

    facet.configure(router.create());

    return facet;
  }
}
