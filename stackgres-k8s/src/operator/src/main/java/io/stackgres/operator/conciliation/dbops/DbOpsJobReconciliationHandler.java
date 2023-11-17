/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.labels.LabelFactoryForDbOps;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.FireAndForgetJobReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresDbOps.class, kind = "Job")
@ApplicationScoped
public class DbOpsJobReconciliationHandler
    extends FireAndForgetJobReconciliationHandler<StackGresDbOps> {

  @Inject
  public DbOpsJobReconciliationHandler(
      @ReconciliationScope(value = StackGresDbOps.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresDbOps> handler,
      LabelFactoryForDbOps labelFactory,
      ResourceFinder<Job> jobFinder,
      ResourceScanner<Pod> podScanner) {
    super(handler, labelFactory, jobFinder, podScanner);
  }

  @Override
  protected boolean isAlreadyCompleted(StackGresDbOps context) {
    return DbOpsUtil.isAlreadyCompleted(context);
  }

}
