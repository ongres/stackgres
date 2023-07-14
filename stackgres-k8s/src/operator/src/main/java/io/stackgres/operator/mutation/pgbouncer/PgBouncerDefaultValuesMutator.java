/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PgBouncerDefaultValuesMutator
    extends AbstractValuesMutator<StackGresPoolingConfig, PoolingReview>
    implements PgBouncerMutator {

  @Inject
  public PgBouncerDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresPoolingConfig> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  protected Class<StackGresPoolingConfig> getResourceClass() {
    return StackGresPoolingConfig.class;
  }

}
