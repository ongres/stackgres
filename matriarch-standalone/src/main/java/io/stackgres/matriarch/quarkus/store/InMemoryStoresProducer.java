package io.stackgres.matriarch.quarkus.store;

import io.stackgres.matriarch.spi.StateStore;
import io.stackgres.matriarch.spi.StatusCache;
import io.stackgres.matriarch.store.InMemoryStateStore;
import io.stackgres.matriarch.store.InMemoryStatusCache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Produces the core's framework-free in-memory {@link StateStore}/{@link StatusCache} defaults as CDI
 * beans for the standalone app. A durable backend swaps in by producing different implementations here.
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
