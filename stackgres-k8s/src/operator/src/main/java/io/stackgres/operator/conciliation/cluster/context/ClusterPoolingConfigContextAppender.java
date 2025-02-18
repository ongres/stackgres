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
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterPoolingConfigContextAppender
    extends ContextAppender<StackGresCluster, Builder> {

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;
  private final DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  public ClusterPoolingConfigContextAppender(
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      DefaultPoolingConfigFactory defaultPoolingConfigFactory) {
    this.poolingConfigFinder = poolingConfigFinder;
    this.defaultPoolingConfigFactory = defaultPoolingConfigFactory;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<StackGresPoolingConfig> poolingConfig = Optional
        .ofNullable(cluster.getSpec().getConfigurations().getSgPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(
                poolingConfigName,
                cluster.getMetadata().getNamespace()));
    if (!cluster.getSpec().getConfigurations().getSgPoolingConfig()
        .equals(defaultPoolingConfigFactory.getDefaultResourceName(cluster))
        && !Optional.ofNullable(cluster.getSpec().getPods().getDisableConnectionPooling()).orElse(false)
        && poolingConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPoolingConfig.KIND + " "
              + cluster.getSpec().getConfigurations().getSgPoolingConfig()
              + " was not found");
    }
    contextBuilder.poolingConfig(poolingConfig);
  }

}
