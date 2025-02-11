/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedClusterCoordinatorPgBouncerDefaultPoolingConfig
    implements ResourceGenerator<StackGresShardedClusterContext> {

  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusterCoordinatorPgBouncerDefaultPoolingConfig(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Stream
        .of(Optional.of(context.getSource().getSpec().getCoordinator().getPods())
            .map(StackGresClusterPods::getDisableConnectionPooling)
            .orElse(false))
        .filter(disabled -> !disabled)
        .filter(ignored -> context.getCoordinatorPostgresConfig().isEmpty()
            || context.getCoordinatorPostgresConfig()
            .map(postgresConfig -> postgresConfig.getMetadata().getOwnerReferences())
            .stream()
            .flatMap(List::stream)
            .anyMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
        .map(ignored -> getDefaultConfig(context.getSource()));
  }

  private StackGresPoolingConfig getDefaultConfig(StackGresShardedCluster cluster) {
    return new StackGresPoolingConfigBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig())
        .withLabels(labelFactory.defaultConfigLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withNewPgBouncer()
        .withNewPgbouncerIni()
        .endPgbouncerIni()
        .endPgBouncer()
        .endSpec()
        .build();
  }

}
