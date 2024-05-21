/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.initialization.PostgresConfigurationFactory;
import io.stackgres.operator.initialization.PostgresDefaultFactoriesProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PgConfigDefaultValuesDelegator implements PgConfigMutator {

  private final PostgresDefaultFactoriesProvider defaultFactoriesProducer;
  private final ObjectMapper objectMapper;

  @Inject
  public PgConfigDefaultValuesDelegator(
      PostgresDefaultFactoriesProvider defaultFactoriesProducer,
      ObjectMapper objectMapper) {
    this.defaultFactoriesProducer = defaultFactoriesProducer;
    this.objectMapper = objectMapper;
  }

  @Override
  public StackGresPostgresConfig mutate(StackGresPostgresConfigReview review, StackGresPostgresConfig resource) {
    Map<String, PostgresConfigurationFactory> factoriesMap = defaultFactoriesProducer
        .getPostgresFactories()
        .stream()
        .collect(Collectors
            .toMap(PostgresConfigurationFactory::getPostgresVersion, Function.identity()));
    return Optional.ofNullable(resource)
        .map(StackGresPostgresConfig::getSpec)
        .map(StackGresPostgresConfigSpec::getPostgresVersion)
        .map(factoriesMap::get)
        .map(factory -> PgConfigDefaultValuesMutator.create(factory, objectMapper))
        .map(mutator -> mutator.mutate(review, resource))
        .orElse(resource);
  }

}
