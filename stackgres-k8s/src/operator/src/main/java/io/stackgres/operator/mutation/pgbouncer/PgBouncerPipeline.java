/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class PgBouncerPipeline
    extends AbstractMutationPipeline<StackGresPoolingConfig, PoolingReview> {

  @Inject
  public PgBouncerPipeline(
      @Any Instance<PgBouncerMutator> mutators) {
    super(mutators);
  }

}
