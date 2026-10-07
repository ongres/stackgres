package io.stackgres.matriarch.quarkus.event;

/**
 * Fired when the slony agent pushes an event (and it's recorded in {@link NodeEventStore}), so the cloud
 * uplink can forward it up as a seq-event — the node-event counterpart of how domain cluster events are
 * forwarded. Lets the cloud serve {@code GetNodeEvents} at parity with a directly-connected matriarch.
 */
public record NodeEventRecorded(NodeEventStore.Stored stored) {
}
