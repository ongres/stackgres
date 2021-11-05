/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.PostgresConfigurationFactory;
import io.stackgres.operator.initialization.PostgresDefaultFactoriesProvider;

@ApplicationScoped
public class DefaultPostgresDelegator implements ClusterMutator {

  @Inject
  PostgresDefaultFactoriesProvider resorceFactoryProducer;

  @Inject
  CustomResourceFinder<StackGresPostgresConfig> finder;

  @Inject
  CustomResourceScheduler<StackGresPostgresConfig> scheduler;

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    return getMutator(review)
        .map(mutator -> mutator.mutate(review))
        .orElseThrow(IllegalStateException::new);
  }

  private Optional<DefaultPostgresMutator> getMutator(StackGresClusterReview review) {
    Map<String, PostgresConfigurationFactory> factoryMap = resorceFactoryProducer
        .getPostgresFactories()
        .stream()
        .collect(Collectors
            .toMap(PostgresConfigurationFactory::getPostgresVersion, Function.identity()));
    StackGresCluster cluster = review.getRequest().getObject();
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .map(getPostgresFlavorComponent(cluster)::findMajorVersion)
        .map(factoryMap::get)
        .map(factory -> {
          try {
            final DefaultPostgresMutator mutator = new DefaultPostgresMutator();
            mutator.setFinder(finder);
            mutator.setScheduler(scheduler);
            mutator.setResourceFactory(factory);
            mutator.init();
            return mutator;
          } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
          }
        });
  }

}
