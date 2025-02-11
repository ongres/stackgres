/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterPoolingConfigContextAppender
    extends ContextAppender<StackGresCluster, StackGresClusterContext.Builder> {

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  public ClusterPoolingConfigContextAppender(CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder) {
    this.poolingConfigFinder = poolingConfigFinder;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<StackGresPoolingConfig> poolingConfig = Optional
        .ofNullable(cluster.getSpec().getConfigurations().getSgPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(
                poolingConfigName,
                cluster.getMetadata().getNamespace()));
    if (!Optional.ofNullable(cluster.getSpec().getPods().getDisableConnectionPooling()).orElse(false)
        && poolingConfig.isEmpty()) {
      throw new IllegalArgumentException(
          "SGCluster " + cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName()
          + " have a non existent SGPoolingConfig "
          + cluster.getSpec().getConfigurations().getSgPoolingConfig());
    }
    contextBuilder.poolingConfig(poolingConfig);
  }

}
