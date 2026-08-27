package io.stackgres.matriarch.quarkus.resources;

/**
 * Fired when this matriarch's slony node set changes (agent attach/detach). The cloud uplink observes
 * it and re-sends a snapshot so the cloud's node view stays fresh — the node analog of a ClusterEvent.
 */
public record NodeInventoryChanged() {
}
