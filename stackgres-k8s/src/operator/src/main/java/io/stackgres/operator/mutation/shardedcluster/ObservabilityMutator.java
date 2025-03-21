/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresClusterObservability;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ObservabilityMutator implements ShardedClusterMutator {

  @Override
  public StackGresShardedCluster mutate(StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    if (resource.getSpec() == null) {
      resource.setSpec(new StackGresShardedClusterSpec());
    }
    if (resource.getSpec().getConfigurations() == null) {
      resource.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    }
    if (resource.getSpec().getConfigurations().getObservability() == null) {
      resource.getSpec().getConfigurations().setObservability(new StackGresClusterObservability());
    }

    var oldObservability = Optional.ofNullable(review.getRequest().getOldObject())
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getObservability);
    if (oldObservability
        .map(StackGresClusterObservability::getDisableMetrics)
        .map(disableMetrics -> disableMetrics.equals(
            resource.getSpec().getConfigurations().getObservability().getDisableMetrics()))
        .orElse(resource.getSpec().getConfigurations().getObservability().getDisableMetrics() == null)) {
      resource.getSpec().getConfigurations().getObservability()
          .setDisableMetrics(Optional.of(resource.getSpec())
              .map(StackGresShardedClusterSpec::getCoordinator)
              .map(StackGresShardedClusterCoordinator::getPods)
              .map(StackGresClusterPods::getDisableMetricsExporter)
              .filter(disableMetricsExporter -> disableMetricsExporter)
              .or(() -> Optional.of(resource.getSpec())
                  .map(StackGresShardedClusterSpec::getShards)
                  .map(StackGresShardedClusterShards::getPods)
                  .map(StackGresClusterPods::getDisableMetricsExporter)
                  .filter(disableMetricsExporter -> disableMetricsExporter))
              .or(() -> Optional.of(resource.getSpec())
                  .map(StackGresShardedClusterSpec::getShards)
                  .map(StackGresShardedClusterShards::getOverrides)
                  .stream()
                  .flatMap(List::stream)
                  .flatMap(override -> Optional.of(override)
                      .map(StackGresShardedClusterShard::getPods)
                      .map(StackGresClusterPods::getDisableMetricsExporter)
                      .filter(disableMetricsExporter -> disableMetricsExporter)
                      .stream())
                  .findFirst())
              .orElse(null));
    }
    Optional.of(resource.getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getPods)
        .ifPresent(pods -> pods.setDisableMetricsExporter(
            resource.getSpec().getConfigurations().getObservability().getDisableMetrics()));
    Optional.of(resource.getSpec())
        .map(StackGresShardedClusterSpec::getShards)
        .map(StackGresShardedClusterShards::getPods)
        .ifPresent(pods -> pods.setDisableMetricsExporter(
            resource.getSpec().getConfigurations().getObservability().getDisableMetrics()));
    Optional.of(resource.getSpec())
        .map(StackGresShardedClusterSpec::getShards)
        .map(StackGresShardedClusterShards::getOverrides)
        .stream()
        .flatMap(List::stream)
        .forEach(override -> Optional.of(override)
            .map(StackGresShardedClusterShard::getPods)
            .ifPresent(pods -> pods.setDisableMetricsExporter(
                resource.getSpec().getConfigurations().getObservability().getDisableMetrics())));

    return resource;
  }

}
