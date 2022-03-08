/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PgBouncerReconciliator {

  private static final Logger LOGGER = LoggerFactory.getLogger(PgBouncerReconciliator.class);

  private final EventController eventController;
  private final boolean pgbouncerReconciliationEnabled;
  private final PgBouncerAuthFileReconciliator authFileReconciliator;

  @Dependent
  public static class Parameters {
    @Inject EventController eventController;
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;
    @Inject ResourceFinder<Secret> secretFinder;
    @Inject PostgresConnectionManager postgresConnectionManager;
  }

  @Inject
  public PgBouncerReconciliator(Parameters parameters) {
    this.eventController = parameters.eventController;
    this.pgbouncerReconciliationEnabled = parameters.propertyContext.getBoolean(
        ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER);
    this.authFileReconciliator = new PgBouncerAuthFileReconciliator(
        parameters.poolingConfigFinder, parameters.secretFinder,
        parameters.postgresConnectionManager, new FileSystemHandler());
  }

  public static PgBouncerReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new PgBouncerReconciliator(parameters.findAny().get());
  }

  public ReconciliationResult<Void> reconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    if (pgbouncerReconciliationEnabled) {
      try {
        authFileReconciliator.updatePgbouncerUsersInAuthFile(context);
      } catch (Exception ex) {
        LOGGER.error("An error occurred while updating pgbouncer auth_file", ex);
        try {
          eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
              "An error occurred while reconciling pgbouncer auth_file: " + ex.getMessage(),
              client);
        } catch (Exception eventEx) {
          LOGGER.error("An error occurred while sending an event", eventEx);
        }
        return new ReconciliationResult<>(ex);
      }
    }
    return new ReconciliationResult<>();
  }

}
