/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.initialization.PostgresConfigurationFactory;
import io.stackgres.operator.initialization.PostgresDefaultFactoriesProvider;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;

@ApplicationScoped
public class PgConfigDefaultStateDelegator implements PgConfigMutator {

  @Inject
  ObjectMapper objectMapper;
  private PostgresDefaultFactoriesProvider defaultFactoriesProducer;

  @Override
  public List<JsonPatchOperation> mutate(PgConfigReview review) {
    Map<String, PostgresConfigurationFactory> factoriesMap = defaultFactoriesProducer
        .getPostgresFactories()
        .stream()
        .collect(Collectors
            .toMap(PostgresConfigurationFactory::getPostgresVersion, Function.identity()));
    return Optional.ofNullable(review.getRequest())
        .map(AdmissionRequest::getObject)
        .map(StackGresPostgresConfig::getSpec)
        .map(StackGresPostgresConfigSpec::getPostgresVersion)
        .map(factoriesMap::get)
        .map(factory -> PgConfigDefaultStateMutator.create(factory, objectMapper))
        .map(mutator -> mutator.mutate(review))
        .orElse(List.of());
  }

  @Inject
  public void setDefaultFactoriesProducer(
      PostgresDefaultFactoriesProvider defaultFactoriesProducer) {
    this.defaultFactoriesProducer = defaultFactoriesProducer;
  }
}
