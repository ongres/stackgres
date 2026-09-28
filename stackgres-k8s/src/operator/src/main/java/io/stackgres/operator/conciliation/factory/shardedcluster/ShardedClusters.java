/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder
public class ShardedClusters implements ResourceGenerator<StackGresShardedClusterContext> {

  final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusters(LabelFactoryForShardedCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedClusterContext context) {
    return Seq.of(Boolean.TRUE)
        .filter(Predicate.not(ignore -> StackGresShardingType.SHARDING_SPHERE.equals(
            StackGresShardingType.fromString(context.getShardedCluster().getSpec().getType()))))
        .<HasMetadata>map(ignore -> context.getCoordinator())
        .map(coordinator -> {
          coordinator.getMetadata().setLabels(labelFactory.coordinatorLabels(context.getSource()));
          return coordinator;
        })
        .append(context.getWorkers().stream()
            .map(workers -> {
              workers.getMetadata().setLabels(labelFactory.workersLabels(context.getSource()));
              return workers;
            }))
        .append(context.getQueryRouters().stream()
            .map(queryRouter -> {
              queryRouter.getMetadata().setLabels(
                  labelFactory.queryRoutersLabels(context.getSource()));
              queryRouter.getMetadata().setAnnotations(
                  withRegisteredAnnotation(context, queryRouter,
                      queryRouter.getMetadata().getAnnotations()));
              return queryRouter;
            }));
  }

  /**
   * Add the annotation that allows the Patroni of a Citus query router to start (see
   * {@code SGCluster.spec.configurations.patroni.startGateAnnotations}) once the coordinator has
   * registered its group in {@code pg_dist_node} without shards.
   */
  private Map<String, String> withRegisteredAnnotation(
      StackGresShardedClusterContext context,
      StackGresCluster queryRouter,
      Map<String, String> annotations) {
    return Optional.of(queryRouter.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getStartGateAnnotations)
        .map(startGateAnnotations -> startGateAnnotations.get(
            StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION))
        .filter(group -> StackGresShardedClusterForCitusUtil.isQueryRouterRegistered(
            context, Integer.parseInt(group)))
        .map(group -> Seq.seq(Optional.ofNullable(annotations).orElse(Map.of()))
            .filter(annotation -> !StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION.equals(
                annotation.v1))
            .append(Tuple.tuple(
                StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION, group))
            .toMap(Tuple2::v1, Tuple2::v2))
        .orElse(annotations);
  }

}
