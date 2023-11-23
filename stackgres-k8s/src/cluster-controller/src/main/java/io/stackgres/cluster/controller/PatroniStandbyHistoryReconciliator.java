/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.common.PatroniUtil.HISTORY_KEY;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniStandbyHistoryReconciliator {

  private static final String EMPTY_JSON_ARRAY = "[]";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatroniStandbyHistoryReconciliator.class);

  private final EventController eventController;
  private final ResourceFinder<Endpoints> endpointsFinder;
  private final ResourceWriter<Endpoints> endpointsWriter;
  private final ObjectMapper objectMapper;
  private final String podName;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
    @Inject ResourceWriter<Endpoints> endpointsWriter;
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject ObjectMapper objectMapper;
  }

  @Inject
  public PatroniStandbyHistoryReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.endpointsFinder = parameters.endpointsFinder;
    this.endpointsWriter = parameters.endpointsWriter;
    this.objectMapper = parameters.objectMapper;
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
      reconcileBootstrappedStandbyLeaderWithAntHistory(context);
    } catch (Exception ex) {
      LOGGER.error("An error occurred while"
          + " reconciling patroni history for standby cluster", ex);
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

  private void reconcileBootstrappedStandbyLeaderWithAntHistory(ClusterContext context) {
    Optional<Endpoints> patroniEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.readWriteName(context.getCluster()),
            context.getCluster().getMetadata().getNamespace());
    Optional<Endpoints> patroniConfigEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.configName(context.getCluster()),
            context.getCluster().getMetadata().getNamespace());
    final boolean isBootstrapped = PatroniUtil.isBootstrapped(patroniConfigEndpoints);
    final boolean isPodPrimary = PatroniUtil.isPrimary(podName, patroniEndpoints);
    final boolean isStandbyCluster =
        PatroniUtil.isStandbyCluster(patroniConfigEndpoints, objectMapper);
    final boolean hasAnyHistory = patroniConfigEndpoints
        .map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(HISTORY_KEY))
        .filter(history -> !EMPTY_JSON_ARRAY.equals(history))
        .isPresent();
    if (isPodPrimary && isBootstrapped && isStandbyCluster && hasAnyHistory) {
      LOGGER.info("Cleaning patroni history for standby cluster");
      endpointsWriter.update(patroniConfigEndpoints.orElseThrow(),
          "{\"metadata\":{\"annotations\":"
              + "{\"" + HISTORY_KEY + "\":\"" + EMPTY_JSON_ARRAY + "\"}}}");
    }
  }

}
