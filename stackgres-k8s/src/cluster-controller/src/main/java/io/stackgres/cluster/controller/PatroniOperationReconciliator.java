/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.PatroniCommandUtil;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
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
   * issued: <string> # the instant the operation has been requested by the operator
   * started: <string> # the instant the operation has been started on the Pod
   * </p>
   *
   * <p>
   * Once the restart has been sent to patroni the instant it has been started is stored in the
   * started field and the annotation is not removed until the restart delay configured in
   * SGCluster.spec.pods.updateStrategy.restartDelay has passed. Since the operator waits for the
   * annotation to be removed before issuing any other restart operation this paces the restart of
   * the Postgres instances.
   * </p>
   */
  private void reconcilePatroniOperation(KubernetesClient client, StackGresClusterContext context) {
    var patroniOperationFound = Optional.ofNullable(client.pods()
        .inNamespace(context.getCluster().getMetadata().getNamespace())
        .withName(podName)
        .get())
        .map(Pod::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.PATRONI_OPERATION_KEY));
    if (patroniOperationFound.isEmpty()) {
      return;
    }
    var patroniOperation = patroniOperationFound
        .map(Unchecked.function(objectMapper::readTree))
        .get();
    final Duration restartDelay = PatroniUtil.getRestartDelay(context.getCluster());
    switch (getRestartAction(patroniOperation, restartDelay, Instant.now())) {
      case NONE:
        break;
      case RESTART: {
        LOGGER.info("Restarting postgres instance through patronictl due to operator request");
        var patroniCtl = this.patroniCtl.binaryInstanceFor(context.getCluster());
        var patroniCredentials = PatroniCommandUtil.getPatorniCredentials(context, secretFinder);
        patroniCtl.restart(patroniCredentials.username(), patroniCredentials.password(), podName);
        final Instant started = Instant.now();
        setPatroniOperationStarted(client, context, started);
        LOGGER.info("Postgres instance restarted, another restart operation will not be allowed before {}",
            started.plus(restartDelay));
        break;
      }
      case WAIT:
        LOGGER.debug("Waiting for the restart delay of {} to allow"
            + " another restart operation to be issued", restartDelay);
        break;
      case COMPLETE:
        removePatroniOperation(client, context);
        break;
      default:
        break;
    }
  }

  /**
   * Indicate what has to be done for a restart operation: perform the restart if it has not been
   * performed yet, wait if the restart delay has not passed since the restart has been performed
   * or complete the operation, by removing the annotation, otherwise.
   */
  static RestartAction getRestartAction(
      JsonNode patroniOperation, Duration restartDelay, Instant now) {
    if (!Objects.equals(Optional.ofNullable(
        patroniOperation.get(StackGresContext.PATRONI_OPERATION_TYPE_FIELD))
        .map(JsonNode::asText)
        .orElse(null),
        StackGresContext.PATRONI_OPERATION_RESTART_TYPE)) {
      return RestartAction.NONE;
    }
    final Optional<Instant> started = Optional.ofNullable(
        patroniOperation.get(StackGresContext.PATRONI_OPERATION_STARTED_FIELD))
        .map(JsonNode::asText)
        .flatMap(PatroniOperationReconciliator::parseInstant);
    if (started.isEmpty()) {
      return RestartAction.RESTART;
    }
    if (started.get().plus(restartDelay).isAfter(now)) {
      return RestartAction.WAIT;
    }
    return RestartAction.COMPLETE;
  }

  enum RestartAction {
    NONE, RESTART, WAIT, COMPLETE;
  }

  private static Optional<Instant> parseInstant(String instant) {
    try {
      return Optional.of(Instant.parse(instant));
    } catch (DateTimeParseException ex) {
      LOGGER.warn("Instant {} is not valid", instant, ex);
      return Optional.empty();
    }
  }

  private void setPatroniOperationStarted(
      KubernetesClient client, StackGresClusterContext context, Instant started) {
    KubernetesClientUtil.retryOnConflict(() -> client.pods()
        .inNamespace(context.getCluster().getMetadata().getNamespace())
        .withName(podName)
        .edit(pod -> {
          var annotations = Optional.of(pod)
              .map(Pod::getMetadata)
              .map(ObjectMeta::getAnnotations)
              .map(HashMap::new)
              .orElseGet(HashMap::new);
          var patroniOperation = Optional.ofNullable(
              annotations.get(StackGresContext.PATRONI_OPERATION_KEY))
              .map(Unchecked.function(objectMapper::readTree))
              .filter(ObjectNode.class::isInstance)
              .map(ObjectNode.class::cast)
              .orElseGet(objectMapper::createObjectNode);
          patroniOperation.put(
              StackGresContext.PATRONI_OPERATION_TYPE_FIELD,
              StackGresContext.PATRONI_OPERATION_RESTART_TYPE);
          patroniOperation.put(
              StackGresContext.PATRONI_OPERATION_STARTED_FIELD, started.toString());
          annotations.put(StackGresContext.PATRONI_OPERATION_KEY, patroniOperation.toString());
          return pod
            .edit()
            .editMetadata()
            .withAnnotations(annotations)
            .endMetadata()
            .build();
        }));
  }

  private void removePatroniOperation(KubernetesClient client, StackGresClusterContext context) {
    KubernetesClientUtil.retryOnConflict(() -> client.pods()
        .inNamespace(context.getCluster().getMetadata().getNamespace())
        .withName(podName)
        .edit(pod -> {
          var annotations = Optional.of(pod)
              .map(Pod::getMetadata)
              .map(ObjectMeta::getAnnotations)
              .map(HashMap::new)
              .orElseGet(HashMap::new);
          annotations.remove(StackGresContext.PATRONI_OPERATION_KEY);
          return pod
            .edit()
            .editMetadata()
            .withAnnotations(annotations)
            .endMetadata()
            .build();
        }));
  }

}
