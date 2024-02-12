/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PostgresBootstrapReconciliator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PostgresBootstrapReconciliator.class);

  private final PatroniCtl patroniCtl;
  private final String podName;

  protected PostgresBootstrapReconciliator(
      PatroniCtl patroniCtl,
      String podName) {
    this.patroniCtl = patroniCtl;
    this.podName = podName;
  }

  public PostgresBootstrapReconciliator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.patroniCtl = null;
    this.podName = null;
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "False positives")
  public ReconciliationResult<Boolean> reconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    try {
      if (context.getCluster().getStatus() != null
          && context.getCluster().getStatus().getArch() != null
          && context.getCluster().getStatus().getOs() != null
          && (
              !Objects.equals(
                  context.getCluster().getStatus().getArch(),
                  ExtensionUtil.OS_DETECTOR.getArch())
              || !Objects.equals(
                  context.getCluster().getStatus().getOs(),
                  ExtensionUtil.OS_DETECTOR.getOs())
              )) {
        throw new IllegalStateException("The cluster was initialized with "
            + context.getCluster().getStatus().getArch()
            + "/" + context.getCluster().getStatus().getOs()
            + " but this instance is " + ExtensionUtil.OS_DETECTOR.getArch()
            + "/" + ExtensionUtil.OS_DETECTOR.getOs());
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
        onClusterBootstrapped(client);
        context.getCluster().getStatus().setArch(ExtensionUtil.OS_DETECTOR.getArch());
        context.getCluster().getStatus().setOs(ExtensionUtil.OS_DETECTOR.getOs());
        LOGGER.info("Setting cluster arch {} and os {}",
            context.getCluster().getStatus().getArch(),
            context.getCluster().getStatus().getOs());
        result = true;
      }
      return new ReconciliationResult<>(result);
    } catch (Exception ex) {
      return new ReconciliationResult<>(false, ex);
    }
  }

  protected abstract void onClusterBootstrapped(KubernetesClient client);

}
