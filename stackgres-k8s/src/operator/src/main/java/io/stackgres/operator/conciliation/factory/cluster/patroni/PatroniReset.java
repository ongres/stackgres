/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.controller.SingleReconciliationCycle;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.RESET_PATRONI)
public class PatroniReset implements ContainerFactory<ClusterContainerContext> {

  private final SingleReconciliationCycle singleReconciliationCycle;

  @Inject
  public PatroniReset(
      @OperatorVersionBinder
      SingleReconciliationCycle singleReconciliationCycle) {
    this.singleReconciliationCycle = singleReconciliationCycle;
  }

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return Optional.of(context.getClusterContext().getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .filter(status -> !Boolean.TRUE.equals(status.getCheck()))
        .isPresent();
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder(singleReconciliationCycle.getContainer(context))
        .withName(StackGresInitContainer.RESET_PATRONI.getName())
        .addToEnv(
            new EnvVarBuilder()
            .withName(ClusterControllerProperty
                .CLUSTER_CONTROLLER_RECONCILE_PATRONI_AFTER_MAJOR_VERSION_UPGRADE
                .getEnvironmentVariableName())
            .withValue(Boolean.TRUE.toString())
            .build())
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of();
  }
}
