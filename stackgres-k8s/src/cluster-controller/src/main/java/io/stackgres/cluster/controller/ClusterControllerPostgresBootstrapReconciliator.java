/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Objects;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterBootstrapEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.OsDetector;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterControllerPostgresBootstrapReconciliator
    extends SafeReconciliator<StackGresClusterContext, Boolean> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PostgresBootstrapReconciliator.class);

  private final EventController eventController;
  private final PatroniCtl patroniCtl;
  private final String podName;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject PatroniCtl patroniCtl;
    @Inject ClusterControllerPropertyContext propertyContext;
  }

  @Inject
  public ClusterControllerPostgresBootstrapReconciliator(Parameters parameters) {
    this.patroniCtl = parameters.patroniCtl;
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.eventController = parameters.eventController;
  }

  @Override
  public ReconciliationResult<Boolean> safeReconcile(KubernetesClient client, StackGresClusterContext context) {
    if (context.getCluster().getStatus() != null
        && context.getCluster().getStatus().getArch() != null
        && context.getCluster().getStatus().getOs() != null
        && (
            !Objects.equals(
                context.getCluster().getStatus().getArch(),
                OsDetector.OS_DETECTOR.getArch())
            || !Objects.equals(
                context.getCluster().getStatus().getOs(),
                OsDetector.OS_DETECTOR.getOs())
            )) {
      throw new IllegalStateException("The cluster was initialized with "
          + context.getCluster().getStatus().getArch()
          + "/" + context.getCluster().getStatus().getOs()
          + " but this instance is " + OsDetector.OS_DETECTOR.getArch()
          + "/" + OsDetector.OS_DETECTOR.getOs());
    }
    var patroniCtl = this.patroniCtl.instanceFor(context.getCluster());
    final boolean isBootstrapped = PatroniUtil.isBootstrapped(patroniCtl);
    if (!isBootstrapped) {
      return new ReconciliationResult<>(false);
    }
    final boolean isPodPrimary = PatroniUtil.isPrimary(podName, patroniCtl);
    boolean result = false;
    if (context.getCluster().getStatus().getPodStatuses()
        .stream()
        .filter(podStatus -> Objects.equals(podStatus.getName(), podName))
        .anyMatch(podStatus -> podStatus.getPrimary() == null
            || isPodPrimary != podStatus.getPrimary().booleanValue())) {
      context.getCluster().getStatus().getPodStatuses()
          .stream()
          .filter(podStatus -> Objects.equals(podStatus.getName(), podName))
          .forEach(podStatus -> podStatus.setPrimary(isPodPrimary));
      LOGGER.info("Setting pod as {}", isPodPrimary ? "primary" : "non primary");
      result = true;
    }
    if (isPodPrimary
        && (context.getCluster().getStatus().getArch() == null
        || context.getCluster().getStatus().getOs() == null)) {
      LOGGER.info("Cluster bootstrap completed");
      if (context.getCluster().getStatus() == null) {
        context.getCluster().setStatus(new StackGresClusterStatus());
      }
      eventController.sendEvent(ClusterBootstrapEventReason.CLUSTER_BOOTSTRAP_COMPLETED,
          "Cluster bootstrap completed", client);
      context.getCluster().getStatus().setArch(OsDetector.OS_DETECTOR.getArch());
      context.getCluster().getStatus().setOs(OsDetector.OS_DETECTOR.getOs());
      LOGGER.info("Setting cluster arch {} and os {}",
          context.getCluster().getStatus().getArch(),
          context.getCluster().getStatus().getOs());
      result = true;
    }
    return new ReconciliationResult<>(result);
  }

}
