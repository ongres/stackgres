package io.stackgres.matriarch.quarkus;

import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.event.Event;
import io.stackgres.matriarch.spi.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * CDI producer that constructs the framework-free {@link Matriarch} core from its
 * SPIs. This is the seam between Quarkus and the core: the core has no CDI
 * knowledge; the wrapper hands it the wired implementations and an outbound event
 * hook that simply fires CDI events (so domain events fan out to any
 * {@code @Observes} consumer — SSE, control.v1, the event log). The standalone
 * bare-metal wrapper would construct the same core from a plain {@code main}.
 */
@ApplicationScoped
public class MatriarchProducer {

    @Inject
    jakarta.enterprise.event.Event<Event> domainEvents;

    @Produces
    @Singleton
    public Matriarch matriarch(StateStore store, StatusCache statusCache, Executor executor, VersionCatalog versionCatalog, ExtensionCatalog extensionCatalog) {
        return new Matriarch(store, statusCache, executor, domainEvents::fire, versionCatalog, extensionCatalog);
    }

}