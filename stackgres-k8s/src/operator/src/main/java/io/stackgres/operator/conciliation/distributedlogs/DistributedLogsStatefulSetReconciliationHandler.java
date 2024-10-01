/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.AbstractStatefulSetWithPrimaryReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresDistributedLogs.class, kind = "StatefulSet")
@ApplicationScoped
public class DistributedLogsStatefulSetReconciliationHandler
    extends AbstractStatefulSetWithPrimaryReconciliationHandler<StackGresDistributedLogs> {

  @Inject
  public DistributedLogsStatefulSetReconciliationHandler(
      @ReconciliationScope(value = StackGresDistributedLogs.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresDistributedLogs> handler,
      LabelFactoryForCluster<StackGresDistributedLogs> labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      PatroniCtl patroniCtl, ObjectMapper objectMapper) {
    super(handler, handler, labelFactory, statefulSetFinder, podScanner, pvcScanner,
        patroniCtl, objectMapper);
  }

  @Override
  protected boolean isPatroniOnKubernetes(StackGresDistributedLogs context) {
    return true;
  }

}
