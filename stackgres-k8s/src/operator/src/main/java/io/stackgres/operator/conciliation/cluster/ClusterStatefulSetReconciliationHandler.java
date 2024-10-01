/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.AbstractStatefulSetWithPrimaryReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresCluster.class, kind = "StatefulSet")
@ApplicationScoped
public class ClusterStatefulSetReconciliationHandler
    extends AbstractStatefulSetWithPrimaryReconciliationHandler<StackGresCluster> {

  @Inject
  public ClusterStatefulSetReconciliationHandler(
      @ReconciliationScope(value = StackGresCluster.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresCluster> handler,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      PatroniCtl patroniCtl, ObjectMapper objectMapper) {
    super(handler, handler, labelFactory, statefulSetFinder, podScanner, pvcScanner,
        patroniCtl, objectMapper);
  }

  ClusterStatefulSetReconciliationHandler(
      ReconciliationHandler<StackGresCluster> handler,
      ReconciliationHandler<StackGresCluster> protectHandler,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      PatroniCtl patroniCtl, ObjectMapper objectMapper) {
    super(handler, protectHandler, labelFactory, statefulSetFinder, podScanner, pvcScanner,
        patroniCtl, objectMapper);
  }

  @Override
  protected boolean isPatroniOnKubernetes(StackGresCluster context) {
    return Optional.ofNullable(context.getSpec().getConfigurations())
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(StackGresClusterPatroniConfig::isPatroniOnKubernetes)
        .orElse(true);
  }

}
