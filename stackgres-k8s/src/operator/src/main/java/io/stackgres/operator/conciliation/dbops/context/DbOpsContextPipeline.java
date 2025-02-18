/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.ContextPipeline;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class DbOpsContextPipeline
    extends ContextPipeline<StackGresDbOps, Builder> {

  public DbOpsContextPipeline(Instance<ContextAppender<StackGresDbOps, Builder>> contextAppenders) {
    super(contextAppenders);
  }

}
