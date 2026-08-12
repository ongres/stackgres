package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.Matriarch;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * The single CDI seam for observation: forwards {@link Observation} events the
 * executors fire onto the framework-free matriarch core's {@code Matriarch.notify*} methods.
 * Keeping this in the adapter is what lets the core stay free of CDI/EE (P2) while the Quarkus app still
 * enjoys decoupled pub/sub.
 */
@ApplicationScoped
public class ObservationBridge {

    @Inject
    Matriarch matriarch;

    void onObservation(@Observes Observation observation) {
        switch (observation) {
            case Observation.Status status -> matriarch.notifyStatus(status.status());
            case Observation.Removed removed -> matriarch.notifyRemoved(removed.id());
            case Observation.Failed failed -> matriarch.notifyFailed(failed.id(), failed.reason());
            case Observation.Pending pending -> matriarch.notifyPending(pending.id(), pending.reason());
            case Observation.Metrics m -> matriarch.notifyMetrics(m.clusterId(), m.instanceId(), m.cpu(), m.memory(), m.storageUsed());
        }
    }

}