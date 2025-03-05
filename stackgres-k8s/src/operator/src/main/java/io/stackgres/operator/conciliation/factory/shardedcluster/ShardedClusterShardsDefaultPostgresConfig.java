/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedClusterShardsDefaultPostgresConfig
    implements ResourceGenerator<StackGresShardedClusterContext> {

  private final LabelFactoryForShardedCluster labelFactory;
  private final DefaultShardedClusterPostgresConfigFactory defaultShardedClusterPostgresConfigFactory;

  @Inject
  public ShardedClusterShardsDefaultPostgresConfig(
      LabelFactoryForShardedCluster labelFactory,
      DefaultShardedClusterPostgresConfigFactory defaultShardedClusterPostgresConfigFactory) {
    this.labelFactory = labelFactory;
    this.defaultShardedClusterPostgresConfigFactory = defaultShardedClusterPostgresConfigFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Stream
        .of(true)
        .filter(ignored -> context.getShardsPostgresConfig().isEmpty()
            || context.getShardsPostgresConfig()
            .filter(postgresConfig -> labelFactory.defaultConfigLabels(context.getSource())
                .entrySet()
                .stream()
                .allMatch(label -> Optional
                    .ofNullable(postgresConfig.getMetadata().getLabels())
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .anyMatch(label::equals)))
            .map(postgresConfig -> postgresConfig.getMetadata().getOwnerReferences())
            .stream()
            .flatMap(List::stream)
            .anyMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
        .filter(ignored -> !context.getSource().getSpec()
            .getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig()
            .equals(context.getSource().getSpec().getShards().getConfigurations().getSgPostgresConfig()))
        .map(ignored -> getDefaultConfig(context.getSource()));
  }

  private StackGresPostgresConfig getDefaultConfig(StackGresShardedCluster cluster) {
    return new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig())
        .withLabels(labelFactory.defaultConfigLabels(cluster))
        .endMetadata()
        .withSpec(defaultShardedClusterPostgresConfigFactory.buildResource(cluster).getSpec())
        .build();
  }

}
