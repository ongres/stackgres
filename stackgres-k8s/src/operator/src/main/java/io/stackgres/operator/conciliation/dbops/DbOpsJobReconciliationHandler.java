/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.LabelFactoryForDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.AbstractJobReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresDbOps.class, kind = "Job")
@ApplicationScoped
public class DbOpsJobReconciliationHandler
    extends AbstractJobReconciliationHandler<StackGresDbOps> {

  @Inject
  public DbOpsJobReconciliationHandler(
      LabelFactoryForDbOps labelFactory,
      ResourceFinder<Job> jobFinder,
      ResourceWriter<Job> jobWriter,
      ResourceScanner<Pod> podScanner,
      ResourceWriter<Pod> podWriter) {
    super(labelFactory, jobFinder, jobWriter, podScanner, podWriter);
  }

  public DbOpsJobReconciliationHandler() {
    super(null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
