/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class DbOpsPipeline extends AbstractMutationPipeline<StackGresDbOps, DbOpsReview> {

  @Inject
  public DbOpsPipeline(
      @Any Instance<DbOpsMutator> mutators) {
    super(mutators);
  }

}
