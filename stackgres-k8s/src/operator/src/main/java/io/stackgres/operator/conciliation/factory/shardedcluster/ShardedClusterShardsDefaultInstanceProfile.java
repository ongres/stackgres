/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileBuilder;
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
public class ShardedClusterShardsDefaultInstanceProfile
    implements ResourceGenerator<StackGresShardedClusterContext> {

  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusterShardsDefaultInstanceProfile(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Stream
        .of(true)
        .filter(ignored -> context.getShardsProfile().isEmpty()
            || context.getShardsProfile()
            .map(instanceProfile -> instanceProfile.getMetadata().getOwnerReferences())
            .stream()
            .flatMap(List::stream)
            .anyMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
        .filter(ignored -> !context.getSource().getSpec().getCoordinator().getSgInstanceProfile()
            .equals(context.getSource().getSpec().getShards().getSgInstanceProfile()))
        .map(ignored -> getDefaultProfile(context.getSource()));
  }

  private StackGresProfile getDefaultProfile(StackGresShardedCluster cluster) {
    return new StackGresProfileBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getSpec().getShards().getSgInstanceProfile())
        .withLabels(labelFactory.defaultConfigLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .endSpec()
        .build();
  }

}
