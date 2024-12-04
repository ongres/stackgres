/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsResharding;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsReshardingCitus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@ShardedDbOpsJob("resharding")
public class ShardedDbOpsReshardingJob extends AbstractShardedDbOpsJob {

  private final LabelFactoryForCluster clusterLabelFactory;

  @Inject
  public ShardedDbOpsReshardingJob(
      ResourceFactory<StackGresShardedDbOpsContext, PodSecurityContext> podSecurityFactory,
      ShardedDbOpsEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactoryForShardedDbOps dbOpsLabelFactory,
      ObjectMapper jsonMapper,
      KubectlUtil kubectl,
      ShardedDbOpsVolumeMounts dbOpsVolumeMounts,
      ShardedDbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory,
      LabelFactoryForCluster clusterLabelFactory) {
    super(podSecurityFactory, clusterStatefulSetEnvironmentVariables,
        dbOpsLabelFactory, jsonMapper, kubectl, dbOpsVolumeMounts, dbOpsTemplatesVolumeFactory);
    this.clusterLabelFactory = clusterLabelFactory;
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresShardedDbOpsContext context) {
    StackGresShardedDbOps dbOps = context.getSource();
    StackGresShardedDbOpsResharding resharding = dbOps.getSpec().getResharding();
    List<EnvVar> runEnvVars = ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
            .withName("CLUSTER_NAMESPACE")
            .withValue(context.getSource().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_NAME")
            .withValue(context.getSource().getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName("SHARDED_CLUSTER_DATABASE")
            .withValue(context.getShardedCluster().getSpec().getDatabase())
            .build(),
            new EnvVarBuilder()
            .withName("COORDINATOR_CLUSTER_LABELS")
            .withValue(clusterLabelFactory.clusterLabelsWithoutUid(
                context.getCoordinatorCluster())
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")))
            .build(),
            new EnvVarBuilder()
            .withName("PATRONI_ROLE_KEY")
            .withValue(PatroniUtil.ROLE_KEY)
            .build(),
            new EnvVarBuilder()
            .withName("PATRONI_PRIMARY_ROLE")
            .withValue(PatroniUtil.PRIMARY_ROLE)
            .build(),
            new EnvVarBuilder()
            .withName("PATRONI_REPLICA_ROLE")
            .withValue(PatroniUtil.REPLICA_ROLE)
            .build(),
            new EnvVarBuilder()
            .withName("PATRONI_CONTAINER_NAME")
            .withValue(StackGresContainer.PATRONI.getName())
            .build())
        .addAll(getReshardingConfigEnvVar(resharding.getCitus()))
        .build();
    return runEnvVars;
  }

  private ImmutableList<EnvVar> getReshardingConfigEnvVar(
      StackGresShardedDbOpsReshardingCitus reshardingConfig) {
    return ImmutableList.of(
        new EnvVarBuilder()
            .withName("THRESHOLD")
            .withValue(Optional.ofNullable(reshardingConfig)
                .map(StackGresShardedDbOpsReshardingCitus::getThreshold)
                .map(String::valueOf)
                .orElse(""))
            .build(),
        new EnvVarBuilder()
            .withName("DRAIN_ONLY")
            .withValue(Optional.ofNullable(reshardingConfig)
                .map(StackGresShardedDbOpsReshardingCitus::getDrainOnly)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
        new EnvVarBuilder()
            .withName("REBALANCE_STRATEGY")
            .withValue(Optional.ofNullable(reshardingConfig)
                .map(StackGresShardedDbOpsReshardingCitus::getRebalanceStrategy)
                .map(rebalanceStrategy -> "'" + rebalanceStrategy + "'")
                .orElse(""))
            .build());
  }

  @Override
  protected ShardedClusterPath getRunScript() {
    return ShardedClusterPath.LOCAL_BIN_RUN_RESHARDING_SH_PATH;
  }

  @Override
  protected String getRunImage(StackGresShardedDbOpsContext context) {
    return kubectl.getImageName(context.getShardedCluster());
  }
}
