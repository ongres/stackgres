package io.stackgres.operator.matriarch;

import io.stackgres.matriarch.spi.StateStore;
import io.stackgres.matriarch.spi.StatusCache;
import io.stackgres.matriarch.store.InMemoryStateStore;
import io.stackgres.matriarch.store.InMemoryStatusCache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Produces the core's framework-free in-memory {@link StateStore}/{@link StatusCache} defaults as CDI
 * beans. Read-only v1 rebuilds the whole view from the SGCluster scan each refresh, so durability
 * isn't needed here; a durable backend swaps in by producing different implementations.
 */
@ApplicationScoped
public class InMemoryStoresProducer {

  @Produces
  @Singleton
  public StateStore stateStore() {
    return new InMemoryStateStore();
  }

  @Produces
  @Singleton
  public StatusCache statusCache() {
    return new InMemoryStatusCache();
  }

}
