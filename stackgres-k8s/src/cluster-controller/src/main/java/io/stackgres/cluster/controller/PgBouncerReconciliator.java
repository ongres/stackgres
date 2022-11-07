/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.ongres.process.FluentProcess;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.ClusterPgBouncerConfigEventReason;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PgBouncerReconciliator {

  private static final Logger LOGGER = LoggerFactory.getLogger(PgBouncerReconciliator.class);
  private static final Pattern PGBOUNCER_COMMAND_PATTERN =
      Pattern.compile("^pgbouncer .*$");
  private static final Path PGBOUNCER_AUTH_PATH =
      Paths.get(ClusterStatefulSetPath.PGBOUNCER_AUTH_FILE_PATH.path());
  private static final Path LAST_PGBOUNCER_AUTH_PATH =
      Paths.get(ClusterStatefulSetPath.PGBOUNCER_AUTH_PATH.path()
          + "/last-" + ClusterStatefulSetPath.PGBOUNCER_AUTH_FILE_PATH.filename());
  private static final Path PGBOUNCER_CONFIG_PATH =
      Paths.get(ClusterStatefulSetPath.PGBOUNCER_CONFIG_FILE_PATH.path());
  private static final Path LAST_PGBOUNCER_CONFIG_PATH =
      Paths.get(ClusterStatefulSetPath.PGBOUNCER_CONFIG_PATH.path()
          + "/last-" + ClusterStatefulSetPath.PGBOUNCER_CONFIG_FILE_PATH.filename());

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
        reconcilePgBouncerConfig(client);
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

  private void reconcilePgBouncerConfig(KubernetesClient client) throws IOException {
    if (configChanged()) {
      reloadPatroniConfig();
      Files.copy(PGBOUNCER_CONFIG_PATH, LAST_PGBOUNCER_CONFIG_PATH,
          StandardCopyOption.REPLACE_EXISTING);
      Files.copy(PGBOUNCER_AUTH_PATH, LAST_PGBOUNCER_AUTH_PATH,
          StandardCopyOption.REPLACE_EXISTING);
      LOGGER.info("PgBouncer config updated");
      eventController.sendEvent(ClusterPgBouncerConfigEventReason.CLUSTER_PGBOUNCER_CONFIG_UPDATED,
          "Patroni config updated", client);
    }
  }

  private boolean configChanged() throws IOException {
    return !Files.exists(LAST_PGBOUNCER_CONFIG_PATH)
        || !Files.exists(LAST_PGBOUNCER_AUTH_PATH)
        || !Seq.seq(Files.readAllLines(LAST_PGBOUNCER_CONFIG_PATH))
        .zipWithIndex()
        .allMatch(Unchecked.predicate(line -> Seq
            .seq(Files.readAllLines(PGBOUNCER_CONFIG_PATH))
            .zipWithIndex()
            .anyMatch(line::equals)))
        || !Seq.seq(Files.readAllLines(LAST_PGBOUNCER_AUTH_PATH))
        .zipWithIndex()
        .allMatch(Unchecked.predicate(line -> Seq
            .seq(Files.readAllLines(PGBOUNCER_AUTH_PATH))
            .zipWithIndex()
            .anyMatch(line::equals)));
  }

  private void reloadPatroniConfig() {
    final String patroniPid = findPatroniPid();
    FluentProcess.start("sh", "-c",
        String.format("kill -s HUP %s", patroniPid)).join();
  }

  private String findPatroniPid() {
    return ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> PGBOUNCER_COMMAND_PATTERN.matcher(command).matches())
            .orElse(false))
        .map(ProcessHandle::pid)
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException(
            "Process with pattern " + PGBOUNCER_COMMAND_PATTERN + " not found"));
  }

}
