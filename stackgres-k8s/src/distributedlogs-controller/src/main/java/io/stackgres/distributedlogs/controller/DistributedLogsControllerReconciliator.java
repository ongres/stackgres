/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.Reconciliator;

@ApplicationScoped
public class DistributedLogsControllerReconciliator
    extends Reconciliator<StackGresDistributedLogsContext> {

  private final DistributedLogsExtensionReconciliator extensionReconciliator;
  private final DistributedLogsClusterReconciliator clusterReconciliator;
  private final CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;

  @Dependent
  public static class Parameters {
    @Inject DistributedLogsExtensionReconciliator extensionReconciliator;
    @Inject DistributedLogsClusterReconciliator clusterReconciliator;
    @Inject CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
  }

  @Inject
  public DistributedLogsControllerReconciliator(Parameters parameters) {
    this.extensionReconciliator = parameters.extensionReconciliator;
    this.clusterReconciliator = parameters.clusterReconciliator;
    this.distributedLogsScheduler = parameters.distributedLogsScheduler;
  }

  public DistributedLogsControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.extensionReconciliator = null;
    this.clusterReconciliator = null;
    this.distributedLogsScheduler = null;
  }

  public static DistributedLogsControllerReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsControllerReconciliator(parameters.findAny().get());
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  @Override
  protected ReconciliationResult<Void> reconcile(KubernetesClient client,
      StackGresDistributedLogsContext context) throws Exception {
    boolean statusUpdated = false;
    ReconciliationResult<Boolean> extensionReconciliationResult =
        extensionReconciliator.reconcile(client, context);
    if (extensionReconciliationResult.result().orElse(false)
        && context.getCluster().getStatus() != null
        && context.getCluster().getStatus().getPodStatuses() != null) {
      if (context.getDistributedLogs().getStatus() == null) {
        context.getDistributedLogs().setStatus(new StackGresDistributedLogsStatus());
      }
      context.getDistributedLogs().getStatus().setPodStatuses(
          context.getCluster().getStatus().getPodStatuses());
    }
    ReconciliationResult<Boolean> clusterReconciliationResult =
        clusterReconciliator.reconcile(client, context);
    statusUpdated = statusUpdated || clusterReconciliationResult.result().orElse(false);
    if (extensionReconciliationResult.result().orElse(false)
        || clusterReconciliationResult.result().orElse(false)) {
      distributedLogsScheduler.updateStatus(context.getDistributedLogs(),
          StackGresDistributedLogs::getStatus,
          StackGresDistributedLogs::setStatus);
    }
    return extensionReconciliationResult.join(clusterReconciliationResult);
  }

}
