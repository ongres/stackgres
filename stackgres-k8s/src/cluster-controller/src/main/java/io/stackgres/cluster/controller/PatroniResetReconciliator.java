/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniResetReconciliator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatroniResetReconciliator.class);

  private final EventController eventController;
  private final PatroniCtl patroniCtl;
  private final ResourceFinder<Secret> secretFinder;
  private final String podName;
  private final boolean isReconcilePatroniAfterMajorVersionUpgrade;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject PatroniCtl patroniCtl;
    @Inject ResourceFinder<Secret> secretFinder;
    @Inject ClusterControllerPropertyContext propertyContext;
  }

  @Inject
  public PatroniResetReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.patroniCtl = parameters.patroniCtl;
    this.secretFinder = parameters.secretFinder;
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.isReconcilePatroniAfterMajorVersionUpgrade = parameters.propertyContext
        .getBoolean(ClusterControllerProperty
            .CLUSTER_CONTROLLER_RECONCILE_PATRONI_AFTER_MAJOR_VERSION_UPGRADE);
  }

  public static PatroniResetReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new PatroniResetReconciliator(parameters.findAny().get());
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
    if (isReconcilePatroniAfterMajorVersionUpgrade
        || (isPodPrimary && isBootstrapped && isStandbyCluster && hasAnyHistory)) {
      if (isReconcilePatroniAfterMajorVersionUpgrade) {
        LOGGER.info("Reset patroni state for major version upgrade");
      } else {
        LOGGER.info("Reset patroni state for standby cluster");
      }
      String clusterName = context.getCluster().getMetadata().getName();
      var credentials = secretFinder.findByNameAndNamespace(
          PatroniUtil.secretName(context.getCluster().getMetadata().getName()),
          context.getCluster().getMetadata().getNamespace())
          .map(Secret::getData)
          .map(ResourceUtil::decodeSecret)
          .map(date -> Tuple.tuple(
              Optional.ofNullable(date.get(StackGresPasswordKeys.RESTAPI_USERNAME_KEY))
              .orElseThrow(() -> new RuntimeException("Can not find key "
                  + StackGresPasswordKeys.RESTAPI_USERNAME_KEY
                  + " in Secret " + PatroniUtil.secretName(clusterName))),
              Optional.ofNullable(date.get(StackGresPasswordKeys.RESTAPI_PASSWORD_KEY))
              .orElseThrow(() -> new RuntimeException("Can not find key "
                  + StackGresPasswordKeys.RESTAPI_PASSWORD_KEY
                  + " in Secret " + PatroniUtil.secretName(clusterName)))))
          .orElseThrow(() -> new RuntimeException(
              "Can not find Secret " + PatroniUtil.secretName(clusterName)));
      patroniCtl.remove(credentials.v1, credentials.v2);
    }
  }

}
