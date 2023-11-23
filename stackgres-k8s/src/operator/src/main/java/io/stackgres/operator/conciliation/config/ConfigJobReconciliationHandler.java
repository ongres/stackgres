/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReplaceWhenUnprocessableJobReconciliationHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresConfig.class, kind = "Job")
@ApplicationScoped
public class ConfigJobReconciliationHandler
    extends ReplaceWhenUnprocessableJobReconciliationHandler<StackGresConfig> {

  @Inject
  public ConfigJobReconciliationHandler(
      @ReconciliationScope(value = StackGresConfig.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresConfig> handler,
      LabelFactoryForConfig labelFactory,
      ResourceFinder<Job> jobFinder,
      ResourceScanner<Pod> podScanner) {
    super(handler, labelFactory, jobFinder, podScanner);
  }

}
