/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterPostgresConfigContextAppender
    extends ContextAppender<StackGresCluster, StackGresClusterContext.Builder> {

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  public ClusterPostgresConfigContextAppender(CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder) {
    this.postgresConfigFinder = postgresConfigFinder;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final StackGresPostgresConfig postgresConfig = postgresConfigFinder
        .findByNameAndNamespace(
            cluster.getSpec().getConfigurations().getSgPostgresConfig(),
            cluster.getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName()
                + " have a non existent SGPostgresConfig "
                + cluster.getSpec().getConfigurations().getSgPostgresConfig()));
    String givenPgVersion = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .orElse(null);
    String clusterMajorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(givenPgVersion);
    String postgresConfigVersion = postgresConfig.getSpec().getPostgresVersion();

    if (!postgresConfigVersion.equals(clusterMajorVersion)) {
      throw new IllegalArgumentException(
          "Invalid postgres version, must be "
              + postgresConfigVersion + " to use SGPostgresConfig "
              + cluster.getSpec().getConfigurations().getSgPostgresConfig());
    }
    contextBuilder.postgresConfig(postgresConfig);
  }

}
