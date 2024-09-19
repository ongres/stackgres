/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresVersion;
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

  private static final long V_1_13 = StackGresVersion.V_1_13.getVersionAsNumber();

  @Override
  public StackGresShardedCluster mutate(StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    long version = StackGresVersion.getStackGresVersionFromResourceAsNumber(resource);
    if (version <= V_1_13) {
      if (resource.getSpec() == null) {
        resource.setSpec(new StackGresShardedClusterSpec());
      }
      if (resource.getSpec().getConfigurations() == null) {
        resource.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
      }
      if (resource.getSpec().getConfigurations().getObservability() == null) {
        resource.getSpec().getConfigurations().setObservability(new StackGresClusterObservability());
      }
      resource.getSpec().getConfigurations().getObservability()
          .setDiableMetrics(Optional.of(resource.getSpec())
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
                  .map(StackGresShardedClusterShard::getPods)
                  .map(StackGresClusterPods::getDisableMetricsExporter)
                  .filter(disableMetricsExporter -> disableMetricsExporter)
                  .findFirst())
              .orElse(null));
      Optional.of(resource.getSpec())
          .map(StackGresShardedClusterSpec::getCoordinator)
          .map(StackGresShardedClusterCoordinator::getPods)
          .ifPresent(pods -> pods.setDisableMetricsExporter(null));
      Optional.of(resource.getSpec())
          .map(StackGresShardedClusterSpec::getShards)
          .map(StackGresShardedClusterShards::getPods)
          .ifPresent(pods -> pods.setDisableMetricsExporter(null));
      Optional.of(resource.getSpec())
          .map(StackGresShardedClusterSpec::getShards)
          .map(StackGresShardedClusterShards::getOverrides)
          .stream()
          .flatMap(List::stream)
          .forEach(override -> Optional.of(override)
              .map(StackGresShardedClusterShard::getPods)
              .ifPresent(pods -> pods.setDisableMetricsExporter(null)));
      resource.getSpec().getConfigurations().getObservability()
          .setPrometheusAutobind(resource.getSpec().getPrometheusAutobind());
      resource.getSpec().setPrometheusAutobind(null);
      Optional.of(resource.getSpec())
          .map(StackGresShardedClusterSpec::getCoordinator)
          .ifPresent(spec -> spec.setPrometheusAutobind(null));
      Optional.of(resource.getSpec())
          .map(StackGresShardedClusterSpec::getShards)
          .ifPresent(spec -> spec.setPrometheusAutobind(null));
      Optional.of(resource.getSpec())
          .map(StackGresShardedClusterSpec::getShards)
          .map(StackGresShardedClusterShards::getOverrides)
          .stream()
          .flatMap(List::stream)
          .forEach(override -> Optional.of(override)
              .ifPresent(spec -> spec.setPrometheusAutobind(null)));
    }
    return resource;
  }

}
