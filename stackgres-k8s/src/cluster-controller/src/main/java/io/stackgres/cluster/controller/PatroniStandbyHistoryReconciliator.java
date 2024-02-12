/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniStandbyHistoryReconciliator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatroniStandbyHistoryReconciliator.class);

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
  public PatroniStandbyHistoryReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.patroniCtl = parameters.patroniCtl;
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
  }

  public static PatroniStandbyHistoryReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new PatroniStandbyHistoryReconciliator(parameters.findAny().get());
  }

  public ReconciliationResult<Void> reconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    try {
      reconcileBootstrappedStandbyLeaderWithAnyHistory(context);
    } catch (Exception ex) {
      LOGGER.error("An error occurred while reconciling patroni history for standby cluster", ex);
      try {
        eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
            "An error occurred while reconciling patroni history for standby cluster: "
                + ex.getMessage(),
            client);
      } catch (Exception eventEx) {
        LOGGER.error("An error occurred while sending an event", eventEx);
      }
      return new ReconciliationResult<>(ex);
    }
    return new ReconciliationResult<>();
  }

  private void reconcileBootstrappedStandbyLeaderWithAnyHistory(ClusterContext context) {
    var patroniCtl = this.patroniCtl.instanceFor(context.getCluster());
    final boolean isBootstrapped = PatroniUtil.isBootstrapped(patroniCtl);
    final boolean isPodPrimary = PatroniUtil.isPrimary(podName, patroniCtl);
    final boolean isStandbyCluster = PatroniUtil.isStandbyCluster(patroniCtl);
    final boolean hasAnyHistory = !patroniCtl.history().isEmpty();
    if (isPodPrimary && isBootstrapped && isStandbyCluster && hasAnyHistory) {
      LOGGER.info("Cleaning patroni history for standby cluster");
      patroniCtl.remove();
    }
  }

}
