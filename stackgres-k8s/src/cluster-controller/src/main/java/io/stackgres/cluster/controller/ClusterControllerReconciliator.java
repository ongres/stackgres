/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.Reconciliator;

@ApplicationScoped
public class ClusterControllerReconciliator
    extends Reconciliator<StackGresClusterContext> {

  private final CustomResourceScheduler<StackGresCluster> clusterScheduler;
  private final ClusterExtensionReconciliator extensionReconciliator;

  @Dependent
  public static class Parameters {
    @Inject CustomResourceScheduler<StackGresCluster> clusterScheduler;
    @Inject ClusterExtensionReconciliator extensionReconciliator;
  }

  @Inject
  public ClusterControllerReconciliator(Parameters parameters) {
    this.clusterScheduler = parameters.clusterScheduler;
    this.extensionReconciliator = parameters.extensionReconciliator;
  }

  public ClusterControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.clusterScheduler = null;
    this.extensionReconciliator = null;
  }

  public static ClusterControllerReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new ClusterControllerReconciliator(parameters.findAny().get());
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  @Override
  protected ReconciliationResult<?> reconcile(KubernetesClient client,
      StackGresClusterContext context) throws Exception {
    ReconciliationResult<Boolean> extensionReconciliationResult =
        extensionReconciliator.reconcile(client, context);
    if (extensionReconciliationResult.result().orElse(false)) {
      clusterScheduler.updateStatus(context.getCluster(),
          StackGresCluster::getStatus,
          StackGresCluster::setStatus);
    }
    return extensionReconciliationResult;
  }

}
