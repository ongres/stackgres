/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;

public class PgConfigDefaultValuesMutator
    extends AbstractValuesMutator<StackGresPostgresConfig, PgConfigReview>
    implements PgConfigMutator {

  public static PgConfigDefaultValuesMutator create(
      DefaultCustomResourceFactory<StackGresPostgresConfig> factory,
      ObjectMapper objectMapper) {
    PgConfigDefaultValuesMutator mutator = new PgConfigDefaultValuesMutator(
        factory, objectMapper);
    mutator.init();
    return mutator;
  }

  private final PgConfigNormalizeValuesMutator normalizeValuesMutator;

  public PgConfigDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresPostgresConfig> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
    this.normalizeValuesMutator = new PgConfigNormalizeValuesMutator();
  }

  public StackGresPostgresConfig mutate(PgConfigReview review, StackGresPostgresConfig resource) {
    resource = super.mutate(review, resource);
    resource = normalizeValuesMutator.mutate(review, resource);
    return resource;
  }

  @Override
  protected Class<StackGresPostgresConfig> getResourceClass() {
    return StackGresPostgresConfig.class;
  }

}
