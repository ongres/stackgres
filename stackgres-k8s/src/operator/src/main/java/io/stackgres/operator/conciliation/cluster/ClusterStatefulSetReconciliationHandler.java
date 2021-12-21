/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.AbstractStatefulSetReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresCluster.class, kind = "StatefulSet")
@ApplicationScoped
public class ClusterStatefulSetReconciliationHandler
    extends AbstractStatefulSetReconciliationHandler<StackGresCluster> {

  @Inject
  public ClusterStatefulSetReconciliationHandler(
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceWriter<StatefulSet> statefulSetWriter,
      ResourceScanner<Pod> podScanner,
      ResourceWriter<Pod> podWriter,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      ResourceWriter<PersistentVolumeClaim> pvcWriter,
      ResourceFinder<Endpoints> endpointsFinder, JsonMapper objectMapper) {
    super(labelFactory, statefulSetFinder, statefulSetWriter, podScanner, podWriter, pvcScanner,
        pvcWriter, endpointsFinder, objectMapper);
  }

  public ClusterStatefulSetReconciliationHandler() {
    super(null, null, null, null, null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
