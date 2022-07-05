/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import static io.stackgres.common.StackGresDistributedLogsUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.PostgresConfigurationFactory;
import io.stackgres.operator.initialization.PostgresDefaultFactoriesProvider;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class DefaultPostgresDelegator implements DistributedLogsMutator {

  @Inject
  PostgresDefaultFactoriesProvider resourceFactoryProducer;

  @Inject
  CustomResourceFinder<StackGresPostgresConfig> finder;

  @Inject
  CustomResourceScheduler<StackGresPostgresConfig> scheduler;

  @Override
  public List<JsonPatchOperation> mutate(StackGresDistributedLogsReview review) {
    return getMutator(review)
        .map(mutator -> mutator.mutate(review))
        .orElseThrow(IllegalStateException::new);
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
        .map(getPostgresFlavorComponent(distributedLogs).get(distributedLogs)::findMajorVersion)
        .map(factoryMap::get)
        .map(Unchecked.function(this::getMutator));
  }

  private DefaultPostgresMutator getMutator(PostgresConfigurationFactory factory)
      throws NoSuchFieldException {
    final DefaultPostgresMutator mutator = new DefaultPostgresMutator();
    mutator.setFinder(finder);
    mutator.setScheduler(scheduler);
    mutator.setResourceFactory(factory);
    mutator.init();
    return mutator;
  }

}
