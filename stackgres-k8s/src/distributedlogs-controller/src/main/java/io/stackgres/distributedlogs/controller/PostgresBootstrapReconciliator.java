/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.distributedlogs.common.ClusterBootstrapEventReason;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PostgresBootstrapReconciliator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PostgresBootstrapReconciliator.class);

  private final EventController eventController;
  private final ResourceFinder<Endpoints> endpointsFinder;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
  }

  @Inject
  public PostgresBootstrapReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.endpointsFinder = parameters.endpointsFinder;
  }

  public static PostgresBootstrapReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new PostgresBootstrapReconciliator(parameters.findAny().get());
  }

  public ReconciliationResult<Boolean> reconcile(KubernetesClient client,
      StackGresDistributedLogsContext context) throws Exception {
    Optional<Endpoints> patroniConfigEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.configName(context.getCluster()),
            context.getCluster().getMetadata().getNamespace());
    if (patroniConfigEndpoints.map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.INITIALIZE_KEY))
        .isPresent()) {
      if (context.getDistributedLogs().getStatus() == null) {
        context.getDistributedLogs().setStatus(new StackGresDistributedLogsStatus());
      }
      LOGGER.info("Cluster bootstrap completed");
      eventController.sendEvent(ClusterBootstrapEventReason.CLUSTER_BOOTSTRAP_COMPLETED,
          "Cluster bootstrap completed", client);
      context.getDistributedLogs().getStatus().setArch(ExtensionUtil.OS_DETECTOR.getArch());
      context.getDistributedLogs().getStatus().setOs(ExtensionUtil.OS_DETECTOR.getOs());
      LOGGER.info("Setting cluster arch {} and os {}",
          context.getDistributedLogs().getStatus().getArch(),
          context.getDistributedLogs().getStatus().getOs());
    }
    return new ReconciliationResult<Boolean>(true);
  }

}
