/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.distributedlogs.configuration.DistributedLogsControllerPropertyContext;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.Reconciliator;

@ApplicationScoped
public class DistributedLogsControllerReconciliator
    extends Reconciliator<StackGresDistributedLogsContext> {

  private final DistributedLogsControllerPostgresBootstrapReconciliator
      postgresBootstrapReconciliator;
  private final DistributedLogsExtensionReconciliator extensionReconciliator;
  private final DistributedLogsClusterReconciliator clusterReconciliator;
  private final CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
  private final DistributedLogsControllerPropertyContext propertyContext;

  @Dependent
  public static class Parameters {
    @Inject DistributedLogsControllerPostgresBootstrapReconciliator postgresBootstrapReconciliator;
    @Inject DistributedLogsExtensionReconciliator extensionReconciliator;
    @Inject DistributedLogsClusterReconciliator clusterReconciliator;
    @Inject CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
    @Inject DistributedLogsControllerPropertyContext propertyContext;
  }

  @Inject
  public DistributedLogsControllerReconciliator(Parameters parameters) {
    this.postgresBootstrapReconciliator = parameters.postgresBootstrapReconciliator;
    this.extensionReconciliator = parameters.extensionReconciliator;
    this.clusterReconciliator = parameters.clusterReconciliator;
    this.distributedLogsScheduler = parameters.distributedLogsScheduler;
    this.propertyContext = parameters.propertyContext;
  }

  public DistributedLogsControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.postgresBootstrapReconciliator = null;
    this.extensionReconciliator = null;
    this.clusterReconciliator = null;
    this.distributedLogsScheduler = null;
    this.propertyContext = null;
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
    ReconciliationResult<Boolean> postgresBootstrapReconciliationResult =
        postgresBootstrapReconciliator.reconcile(client, context);
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
    if (postgresBootstrapReconciliationResult.result().orElse(false)
        || extensionReconciliationResult.result().orElse(false)
        || clusterReconciliationResult.result().orElse(false)) {
      final String podName = propertyContext.getString(
          DistributedLogsControllerProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME);
      distributedLogsScheduler.update(context.getDistributedLogs(),
          (targetDistributedLogs, distributedLogsWithStatus) -> {
            var podStatus = Optional.ofNullable(distributedLogsWithStatus.getStatus())
                .map(StackGresDistributedLogsStatus::getPodStatuses)
                .flatMap(podStatuses -> findPodStatus(podStatuses, podName))
                .orElseThrow();
            if (targetDistributedLogs.getStatus() == null) {
              targetDistributedLogs.setStatus(new StackGresDistributedLogsStatus());
            }
            targetDistributedLogs.getStatus().setArch(
                distributedLogsWithStatus.getStatus().getArch());
            targetDistributedLogs.getStatus().setOs(
                distributedLogsWithStatus.getStatus().getOs());
            if (targetDistributedLogs.getStatus().getPodStatuses() == null) {
              targetDistributedLogs.getStatus().setPodStatuses(new ArrayList<>());
            }
            findPodStatus(targetDistributedLogs.getStatus().getPodStatuses(), podName)
                .ifPresentOrElse(
                    targetPodStatus -> {
                      targetDistributedLogs.getStatus().getPodStatuses().set(
                          targetDistributedLogs.getStatus().getPodStatuses()
                          .indexOf(targetPodStatus),
                          podStatus);
                    },
                    () -> targetDistributedLogs.getStatus().getPodStatuses().add(podStatus));
          });
    }
    return postgresBootstrapReconciliationResult
        .join(extensionReconciliationResult)
        .join(clusterReconciliationResult);
  }

  private Optional<StackGresClusterPodStatus> findPodStatus(
      List<StackGresClusterPodStatus> podStatuses,
      String podName) {
    return podStatuses.stream()
        .filter(podStatus -> podStatus.getName().equals(podName))
        .findFirst();
  }

}
