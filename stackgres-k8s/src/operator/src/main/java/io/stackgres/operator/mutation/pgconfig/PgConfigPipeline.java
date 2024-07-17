/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class PgConfigPipeline
    extends AbstractMutationPipeline<StackGresPostgresConfig, StackGresPostgresConfigReview> {

  @Inject
  public PgConfigPipeline(
      @Any Instance<PgConfigMutator> mutators) {
    super(mutators);
  }

}
