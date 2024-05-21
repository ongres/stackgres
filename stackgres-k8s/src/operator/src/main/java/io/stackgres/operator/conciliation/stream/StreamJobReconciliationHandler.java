/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.StreamUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.FireAndForgetJobReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresStream.class, kind = "Job")
@ApplicationScoped
public class StreamJobReconciliationHandler
    extends FireAndForgetJobReconciliationHandler<StackGresStream> {

  @Inject
  public StreamJobReconciliationHandler(
      @ReconciliationScope(value = StackGresStream.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresStream> handler,
      ResourceFinder<Job> jobFinder,
      ResourceScanner<Pod> podScanner) {
    super(handler, jobFinder, podScanner);
  }

  @Override
  protected boolean canForget(StackGresStream context, HasMetadata resource) {
    return StreamUtil.isAlreadyCompleted(context);
  }

}
