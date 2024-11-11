/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import static io.stackgres.operator.common.StackGresDistributedLogsUtil.getPostgresFlavorComponent;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.StackGresDistributedLogsUtil;
import io.stackgres.operator.initialization.PostgresConfigurationFactory;
import io.stackgres.operator.initialization.PostgresDefaultFactoriesProvider;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class DefaultPostgresDelegator implements DistributedLogsMutator {

  static final long V_1_14 = StackGresVersion.V_1_14.getVersionAsNumber();

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
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      final long versionAsNumber = StackGresVersion.getStackGresVersionAsNumber(
          review.getRequest().getObject());
      if (versionAsNumber <= V_1_14) {
        if (resource.getStatus() == null) {
          resource.setStatus(new StackGresDistributedLogsStatus());
        }
        resource.getStatus().setPostgresVersion("12");
        resource.getStatus().setTimescaledbVersion("1.7.4");
      }
    }
    return getMutator(review, resource)
        .map(mutator -> mutator.mutate(review, resource))
        .orElseThrow(IllegalArgumentException::new);
  }

  private Optional<DefaultPostgresMutator> getMutator(
      StackGresDistributedLogsReview review, StackGresDistributedLogs resource) {
    Map<String, PostgresConfigurationFactory> factoryMap = resourceFactoryProducer
        .getPostgresFactories()
        .stream()
        .collect(Collectors
            .toMap(PostgresConfigurationFactory::getPostgresVersion, Function.identity()));
    return Optional.of(resource)
        .map(StackGresDistributedLogsUtil::getPostgresVersion)
        .map(getPostgresFlavorComponent(resource).get(resource)::getMajorVersion)
        .map(factoryMap::get)
        .map(Unchecked.function(this::getMutator));
  }

  private DefaultPostgresMutator getMutator(PostgresConfigurationFactory factory) {
    return new DefaultPostgresMutator(factory, finder, scheduler);
  }

}
