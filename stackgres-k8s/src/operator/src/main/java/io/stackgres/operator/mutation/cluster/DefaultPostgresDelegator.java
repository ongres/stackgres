/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.PostgresConfigurationFactory;
import io.stackgres.operator.initialization.PostgresDefaultFactoriesProvider;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class DefaultPostgresDelegator implements ClusterMutator {

  private final PostgresDefaultFactoriesProvider resourceFactoryProducer;

  private final CustomResourceFinder<StackGresPostgresConfig> finder;

  private final CustomResourceScheduler<StackGresPostgresConfig> scheduler;

  @Inject
  public DefaultPostgresDelegator(PostgresDefaultFactoriesProvider resourceFactoryProducer,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    this.resourceFactoryProducer = resourceFactoryProducer;
    this.finder = finder;
    this.scheduler = scheduler;
  }

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    return getMutator(review)
        .map(mutator -> mutator.mutate(review, resource))
        .orElseThrow(IllegalStateException::new);
  }

  private Optional<DefaultPostgresMutator> getMutator(StackGresClusterReview review) {
    Map<String, PostgresConfigurationFactory> factoryMap = resourceFactoryProducer
        .getPostgresFactories()
        .stream()
        .collect(Collectors
            .toMap(PostgresConfigurationFactory::getPostgresVersion, Function.identity()));
    StackGresCluster cluster = review.getRequest().getObject();
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .map(getPostgresFlavorComponent(cluster).get(cluster)::getMajorVersion)
        .map(factoryMap::get)
        .map(Unchecked.function(this::getMutator));
  }

  private DefaultPostgresMutator getMutator(PostgresConfigurationFactory factory) {
    final DefaultPostgresMutator mutator = new DefaultPostgresMutator(
        factory, finder, scheduler);
    return mutator;
  }

}
