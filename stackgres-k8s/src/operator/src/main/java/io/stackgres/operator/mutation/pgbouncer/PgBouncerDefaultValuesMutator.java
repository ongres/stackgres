/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PgBouncerDefaultValuesMutator
    extends AbstractValuesMutator<StackGresPoolingConfig, StackGresPoolingConfigReview, HasMetadata>
    implements PgBouncerMutator {

  @Inject
  public PgBouncerDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresPoolingConfig, HasMetadata> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
  }

  @Override
  protected HasMetadata createSourceResource(StackGresPoolingConfig resource) {
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withName(resource.getMetadata().getName())
        .endMetadata()
        .build();
  }

  @Override
  protected Class<StackGresPoolingConfig> getResourceClass() {
    return StackGresPoolingConfig.class;
  }

}
