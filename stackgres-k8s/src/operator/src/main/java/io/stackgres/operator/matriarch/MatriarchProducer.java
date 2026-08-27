/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.event.Event;
import io.stackgres.matriarch.spi.Executor;
import io.stackgres.matriarch.spi.ExtensionCatalog;
import io.stackgres.matriarch.spi.StateStore;
import io.stackgres.matriarch.spi.StatusCache;
import io.stackgres.matriarch.spi.VersionCatalog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Constructs the framework-free Matriarch core from its SPIs and exposes it as a CDI bean — the seam
 * between Quarkus and the core (mirrors the standalone app's producer). The outbound event hook fires
 * CDI events so domain {@code ClusterEvent}s fan out to observers (e.g. {@link ClusterEventStore}).
 */
@ApplicationScoped
public class MatriarchProducer {

  @Inject
  jakarta.enterprise.event.Event<Event> domainEvents;

  @Produces
  @Singleton
  public Matriarch matriarch(StateStore store, StatusCache statusCache, Executor executor,
      VersionCatalog versionCatalog, ExtensionCatalog extensionCatalog) {
    return new Matriarch(store, statusCache, executor, domainEvents::fire, versionCatalog, extensionCatalog);
  }
}
