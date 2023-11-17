/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class ShardedClusterEndpoints implements ResourceGenerator<StackGresShardedClusterContext> {

  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusterEndpoints(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    Seq<HasMetadata> endpoints = Seq.of();

    if (Optional.of(context.getSource().getSpec()
        .getPostgresServices().getCoordinator().getPrimary())
        .map(StackGresPostgresService::getEnabled)
        .orElse(true)) {
      endpoints = endpoints.append(getPrimaryEndpoints(context));
    }

    if (Optional.of(context.getSource().getSpec().getPostgresServices().getShards().getPrimaries())
        .map(StackGresPostgresService::getEnabled)
        .orElse(true)) {
      endpoints = endpoints.append(getShardsEndpoints(context));
    }

    return endpoints;
  }

  private Stream<HasMetadata> getPrimaryEndpoints(StackGresShardedClusterContext context) {
    return Stream.of(new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(context.getSource().getMetadata().getNamespace())
        .withName(StackGresShardedClusterUtil.primaryCoordinatorServiceName(
            context.getSource()))
        .addToLabels(labelFactory.genericLabels(context.getSource()))
        .endMetadata()
        .addAllToSubsets(context.getCoordinatorPrimaryEndpoints()
            .map(Endpoints::getSubsets)
            .orElse(List.of()))
        .build());
  }

  private Stream<HasMetadata> getShardsEndpoints(StackGresShardedClusterContext context) {
    return Stream.of(new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(context.getSource().getMetadata().getNamespace())
        .withName(StackGresShardedClusterUtil.primariesShardsServiceName(
            context.getSource()))
        .addToLabels(labelFactory.genericLabels(context.getSource()))
        .endMetadata()
        .addAllToSubsets(context.getShardsPrimaryEndpoints()
            .stream()
            .map(Endpoints::getSubsets)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList())
        .build());
  }

}
