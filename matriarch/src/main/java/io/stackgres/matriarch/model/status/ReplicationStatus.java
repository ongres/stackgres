package io.stackgres.matriarch.model.status;

/**
 * Observed replication role (reported by slon). Standalone reports STANDALONE.
 */
public enum ReplicationStatus {UNKNOWN, STANDALONE, PRIMARY, REPLICA}
