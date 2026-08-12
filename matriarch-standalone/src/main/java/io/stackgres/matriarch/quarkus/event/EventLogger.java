package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.event.ClusterEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

/**
 * Demonstrates domain-event dispatch: logs every {@link ClusterEvent} the matriarch
 * raises. The same CDI channel is where the SSE relay and the control.v1 Event push
 * will subscribe, and where a StateStore subscriber will persist the event history.
 */
@ApplicationScoped
public class EventLogger {

    private static final Logger LOG = Logger.getLogger(EventLogger.class);

    void onClusterEvent(@Observes ClusterEvent event) {
        LOG.infof("domain event: %s", event);
    }

}