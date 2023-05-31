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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
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
  private final String podName;

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
    this.podName = parameters.propertyContext
        .getString(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME);
  }

  public DistributedLogsControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.postgresBootstrapReconciliator = null;
    this.extensionReconciliator = null;
    this.clusterReconciliator = null;
    this.distributedLogsScheduler = null;
    this.podName = null;
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
    final StackGresCluster cluster = context.getCluster();
    final boolean podStatusMissing = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .map(StackGresClusterPodStatus::getName)
        .noneMatch(podName::equals);
    if (podStatusMissing) {
      if (cluster.getStatus() == null) {
        cluster.setStatus(new StackGresClusterStatus());
      }
      if (cluster.getStatus().getPodStatuses() == null) {
        cluster.getStatus().setPodStatuses(new ArrayList<>());
      }
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName(podName);
      podStatus.setPrimary(false);
      podStatus.setPendingRestart(false);
      cluster.getStatus().getPodStatuses().add(podStatus);
    }

    ReconciliationResult<Boolean> postgresBootstrapReconciliationResult =
        postgresBootstrapReconciliator.reconcile(client, context);
    ReconciliationResult<Boolean> extensionReconciliationResult =
        extensionReconciliator.reconcile(client, context);
    if (context.getCluster().getStatus() != null) {
      if (context.getCluster().getStatus().getPodStatuses() != null) {
        if (context.getDistributedLogs().getStatus() == null) {
          context.getDistributedLogs().setStatus(new StackGresDistributedLogsStatus());
        }
        context.getDistributedLogs().getStatus().setPodStatuses(
            context.getCluster().getStatus().getPodStatuses());
      }
      if (context.getCluster().getStatus().getOs() != null) {
        context.getDistributedLogs().getStatus().setOs(
            context.getCluster().getStatus().getOs());
      }
      if (context.getCluster().getStatus().getArch() != null) {
        context.getDistributedLogs().getStatus().setArch(
            context.getCluster().getStatus().getArch());
      }
    }
    ReconciliationResult<Boolean> clusterReconciliationResult =
        clusterReconciliator.reconcile(client, context);
    if (postgresBootstrapReconciliationResult.result().orElse(false)
        || extensionReconciliationResult.result().orElse(false)
        || clusterReconciliationResult.result().orElse(false)) {
      distributedLogsScheduler.update(context.getDistributedLogs(),
          (currentDistributedLogs) -> {
            var podStatus = Optional.ofNullable(context.getDistributedLogs().getStatus())
                .map(StackGresDistributedLogsStatus::getPodStatuses)
                .flatMap(podStatuses -> findPodStatus(podStatuses, podName))
                .orElseThrow();
            if (currentDistributedLogs.getStatus() == null) {
              currentDistributedLogs.setStatus(new StackGresDistributedLogsStatus());
            }
            currentDistributedLogs.getStatus().setArch(
                context.getDistributedLogs().getStatus().getArch());
            currentDistributedLogs.getStatus().setOs(
                context.getDistributedLogs().getStatus().getOs());
            if (currentDistributedLogs.getStatus().getPodStatuses() == null) {
              currentDistributedLogs.getStatus().setPodStatuses(new ArrayList<>());
            }
            findPodStatus(currentDistributedLogs.getStatus().getPodStatuses(), podName)
                .ifPresentOrElse(
                    targetPodStatus -> {
                      currentDistributedLogs.getStatus().getPodStatuses().set(
                          currentDistributedLogs.getStatus().getPodStatuses()
                          .indexOf(targetPodStatus),
                          podStatus);
                    },
                    () -> currentDistributedLogs.getStatus().getPodStatuses().add(podStatus));
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
