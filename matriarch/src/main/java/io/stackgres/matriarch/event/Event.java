package io.stackgres.matriarch.event;

import java.time.Instant;

/**
 * Root of the matriarch's domain event hierarchy (§5.2). Plain Java: the matriarch
 * <em>raises</em> these; the wiring (CDI in the Quarkus adapter) dispatches them,
 * and a StateStore subscriber will persist a bounded history for {@code
 * GetClusterEvents}. The library neither dispatches nor stores.
 */
public sealed interface Event permits ClusterEvent {

    Instant timestamp();

}