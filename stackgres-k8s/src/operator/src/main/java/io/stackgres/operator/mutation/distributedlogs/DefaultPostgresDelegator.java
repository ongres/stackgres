/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import static io.stackgres.common.StackGresDistributedLogsUtil.getPostgresFlavorComponent;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.PostgresConfigurationFactory;
import io.stackgres.operator.initialization.PostgresDefaultFactoriesProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class DefaultPostgresDelegator implements DistributedLogsMutator {

  private final PostgresDefaultFactoriesProvider resourceFactoryProducer;

  private final CustomResourceFinder<StackGresPostgresConfig> finder;

  private final CustomResourceScheduler<StackGresPostgresConfig> scheduler;

  @Inject
  public DefaultPostgresDelegator(
      PostgresDefaultFactoriesProvider resourceFactoryProducer,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    this.resourceFactoryProducer = resourceFactoryProducer;
    this.finder = finder;
    this.scheduler = scheduler;
  }

  @Override
  public StackGresDistributedLogs mutate(
      StackGresDistributedLogsReview review, StackGresDistributedLogs resource) {
    return getMutator(review)
        .map(mutator -> mutator.mutate(review, resource))
        .orElseThrow(IllegalArgumentException::new);
  }

  private Optional<DefaultPostgresMutator> getMutator(StackGresDistributedLogsReview review) {
    Map<String, PostgresConfigurationFactory> factoryMap = resourceFactoryProducer
        .getPostgresFactories()
        .stream()
        .collect(Collectors
            .toMap(PostgresConfigurationFactory::getPostgresVersion, Function.identity()));
    StackGresDistributedLogs distributedLogs = review.getRequest().getObject();
    return Optional.of(distributedLogs)
        .map(StackGresDistributedLogsUtil::getPostgresVersion)
        .map(getPostgresFlavorComponent(distributedLogs).get(distributedLogs)::getMajorVersion)
        .map(factoryMap::get)
        .map(Unchecked.function(this::getMutator));
  }

  private DefaultPostgresMutator getMutator(PostgresConfigurationFactory factory) {
    return new DefaultPostgresMutator(factory, finder, scheduler);
  }

}
