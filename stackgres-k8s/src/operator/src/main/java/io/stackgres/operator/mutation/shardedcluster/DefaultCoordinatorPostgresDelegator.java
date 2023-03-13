/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.PostgresConfigurationFactory;
import io.stackgres.operator.initialization.PostgresDefaultFactoriesProvider;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class DefaultCoordinatorPostgresDelegator implements ShardedClusterMutator {

  @Inject
  PostgresDefaultFactoriesProvider resourceFactoryProducer;

  @Inject
  CustomResourceFinder<StackGresPostgresConfig> finder;

  @Inject
  CustomResourceScheduler<StackGresPostgresConfig> scheduler;

  @Override
  public List<JsonPatchOperation> mutate(StackGresShardedClusterReview review) {
    return getMutator(review)
        .map(mutator -> mutator.mutate(review))
        .orElseThrow(IllegalStateException::new);
  }

  private Optional<DefaultCoordinatorPostgresMutator> getMutator(
      StackGresShardedClusterReview review) {
    Map<String, PostgresConfigurationFactory> factoryMap = resourceFactoryProducer
        .getPostgresFactories()
        .stream()
        .collect(Collectors
            .toMap(PostgresConfigurationFactory::getPostgresVersion, Function.identity()));
    StackGresShardedCluster cluster = review.getRequest().getObject();
    return Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .map(getPostgresFlavorComponent(cluster).get(cluster)::getMajorVersion)
        .map(factoryMap::get)
        .map(Unchecked.function(this::getMutator));
  }

  private DefaultCoordinatorPostgresMutator getMutator(PostgresConfigurationFactory factory) {
    final DefaultCoordinatorPostgresMutator mutator = new DefaultCoordinatorPostgresMutator(
        factory, finder, scheduler);
    mutator.init();
    return mutator;
  }

}
