/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.PatroniCommandUtil;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniOperationReconciliator extends SafeReconciliator<StackGresClusterContext, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniOperationReconciliator.class);

  private Supplier<Boolean> reconcilePatroni;
  private final String podName;
  private final EventController eventController;
  private final PatroniCtl patroniCtl;
  private final SecretFinder secretFinder;
  private final ObjectMapper objectMapper;

  @Dependent
  public static class Parameters {
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject EventController eventController;
    @Inject PatroniCtl patroniCtl;
    @Inject SecretFinder secretFinder;
    @Inject ObjectMapper objectMapper;
  }

  @Inject
  public PatroniOperationReconciliator(Parameters parameters) {
    this.reconcilePatroni = () -> parameters.propertyContext
        .getBoolean(ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PATRONI);
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.eventController = parameters.eventController;
    this.patroniCtl = parameters.patroniCtl;
    this.secretFinder = parameters.secretFinder;
    this.objectMapper = parameters.objectMapper;
  }

  @Override
  public ReconciliationResult<Void> safeReconcile(KubernetesClient client,
      StackGresClusterContext context) {
    if (!reconcilePatroni.get()) {
      return new ReconciliationResult<>();
    }
    try {
      reconcilePatroniOperation(client, context);
      return new ReconciliationResult<>();
    } catch (RuntimeException ex) {
      LOGGER.error("An error occurred while reconciling patroni operation", ex);
      try {
        eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
            "An error occurred while reconciling patroni operation: " + ex.getMessage(),
            client);
      } catch (Exception eventEx) {
        LOGGER.error("An error occurred while sending an event", eventEx);
      }
      return new ReconciliationResult<>(ex);
    }
  }

  /**
   * A patroni operation allow to perform patronictl command invocation.
   * 
   * <p>
   * The operation is written in the Pod annotations under stackgres.io/patroni-operation.
   * The operation payload is as follow:
   * type: <string> # restart
   * </p>
   */
  private void reconcilePatroniOperation(KubernetesClient client, StackGresClusterContext context) {
    var patroniOperationFound = Optional.ofNullable(client.pods()
        .inNamespace(context.getCluster().getMetadata().getNamespace())
        .withName(podName)
        .get())
        .map(Pod::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresKeys.PATRONI_OPERATION_KEY));
    if (patroniOperationFound.isEmpty()) {
      return;
    }
    var patroniOperation = patroniOperationFound
        .map(Unchecked.function(objectMapper::readTree))
        .get();
    if (Objects.equals(Optional.ofNullable(
        patroniOperation.get("type"))
        .map(JsonNode::asText)
        .orElse(null),
        "restart")) {
      LOGGER.info("Restarting postgres instance through patronictl due to operator request");
      var patroniCtl = this.patroniCtl.binaryInstanceFor(context.getCluster());
      var patroniCredentials = PatroniCommandUtil.getPatorniCredentials(context, secretFinder);
      patroniCtl.restart(patroniCredentials.username(), patroniCredentials.password(), podName);
      KubernetesClientUtil.retryOnConflict(() -> client.pods()
          .inNamespace(context.getCluster().getMetadata().getNamespace())
          .withName(podName)
          .edit(pod -> {
            var annotations = Optional.of(pod)
                .map(Pod::getMetadata)
                .map(ObjectMeta::getAnnotations)
                .map(HashMap::new)
                .orElseGet(HashMap::new);
            annotations.remove(StackGresKeys.PATRONI_OPERATION_KEY);
            return pod
              .edit()
              .editMetadata()
              .withAnnotations(annotations)
              .endMetadata()
              .build();
          }));
    }
  }

}
