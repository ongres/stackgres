/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
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
public class ShardedClusterShardsDefaultPostgresConfig
    implements ResourceGenerator<StackGresShardedClusterContext> {

  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusterShardsDefaultPostgresConfig(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Stream
        .of(true)
        .filter(ignored -> context.getShardsPostgresConfig().isEmpty()
            || context.getShardsPostgresConfig()
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
        .withNewSpec()
        .withPostgresVersion(getPostgresMajorVersion(cluster))
        .endSpec()
        .build();
  }

  private String getPostgresMajorVersion(StackGresShardedCluster cluster) {
    String version = cluster.getSpec().getPostgres().getVersion();
    return version.split("\\.")[0];
  }

}
