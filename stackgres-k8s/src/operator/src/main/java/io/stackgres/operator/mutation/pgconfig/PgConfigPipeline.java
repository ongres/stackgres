/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class PgConfigPipeline
    extends AbstractMutationPipeline<StackGresPostgresConfig, PgConfigReview> {

  @Inject
  public PgConfigPipeline(
      @Any Instance<PgConfigMutator> mutators) {
    super(mutators);
  }

}
