/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PostgresBootstrapReconciliator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PostgresBootstrapReconciliator.class);

  private final ResourceFinder<Endpoints> endpointsFinder;
  private final String podName;

  protected PostgresBootstrapReconciliator(
      ResourceFinder<Endpoints> endpointsFinder,
      String podName) {
    this.endpointsFinder = endpointsFinder;
    this.podName = podName;
  }

  public PostgresBootstrapReconciliator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.endpointsFinder = null;
    this.podName = null;
  }

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
      final Optional<Endpoints> patroniConfigEndpoints = endpointsFinder
          .findByNameAndNamespace(PatroniUtil.configName(context.getCluster()),
              context.getCluster().getMetadata().getNamespace());
      final boolean isBootstrapped = PatroniUtil.isBootstrapped(patroniConfigEndpoints);
      if (!isBootstrapped) {
        return new ReconciliationResult<>(false);
      }
      LOGGER.info("Cluster bootstrap completed");
      final Optional<Endpoints> patroniEndpoints = endpointsFinder
          .findByNameAndNamespace(PatroniUtil.readWriteName(context.getCluster()),
              context.getCluster().getMetadata().getNamespace());
      final boolean isPodPrimary = PatroniUtil.isPrimary(podName, patroniEndpoints);
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
      if (isPodPrimary) {
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
