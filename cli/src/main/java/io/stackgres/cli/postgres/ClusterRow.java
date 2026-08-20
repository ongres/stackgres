package io.stackgres.cli.postgres;

import io.stackgres.postgres.PostgresCluster;

/**
 * One row of {@code cluster list}: a cluster plus the environment it lives in. Kept CLI-side (rather
 * than on the shared {@link PostgresCluster} model) so the aggregated view can render {@code env/name}
 * and an ENVIRONMENT column when the listing spans more than one environment. {@code environmentId} may
 * be empty when the server didn't stamp one (a single-environment local matriarch).
 */
public record ClusterRow(String environmentId, PostgresCluster cluster) {
}
