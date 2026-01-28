/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import static io.stackgres.common.ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_MANAGED_SQL;
import static io.stackgres.common.ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PATRONI;
import static io.stackgres.common.ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER;
import static io.stackgres.common.ClusterControllerProperty.CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.stackgres.cluster.controller.ClusterControllerReconciliationCycle;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle.ReconciliationCycleResult;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusMain
public class StackGresClusterControllerMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StackGresClusterControllerMain.class);

  private static final Path PATRONI_START_FILE_PATH =
      Paths.get(ClusterPath.PATRONI_START_FILE_PATH.path());

  public static void main(String... args) {
    AtomicReference<Tuple2<Integer, Throwable>> exitCodeReference =
        new AtomicReference<>(Tuple.tuple(0, null));
    Quarkus.run(StackGresClusterControllerApp.class,
        (exitCode, throwable) -> exitCodeReference.set(Tuple.tuple(exitCode, throwable)),
        args);
    if (exitCodeReference.get().v1 != 0) {
      throw new RuntimeException("exit code " + exitCodeReference.get(),
          exitCodeReference.get().v2);
    }
  }

  public static class StackGresClusterControllerApp implements QuarkusApplication {

    private final ClusterControllerReconciliationCycle reconciliationCycle;
    private final Event<StackGresClusterControllerAppStartupEvent> startupEvent;
    private final Event<StackGresClusterControllerAppShutdownEvent> shutdownEvent;

    @Inject
    public StackGresClusterControllerApp(
        ClusterControllerReconciliationCycle reconciliationCycle,
        Event<StackGresClusterControllerAppStartupEvent> startupEvent,
        Event<StackGresClusterControllerAppShutdownEvent> shutdownEvent) {
      this.reconciliationCycle = reconciliationCycle;
      this.startupEvent = startupEvent;
      this.shutdownEvent = shutdownEvent;
    }

    @Override
    public int run(String... args) throws Exception {
      if (isReconciliationCycle(args)) {
        runSingleReconciliationCycle();

        return 0;
      }

      if (!Files.exists(PATRONI_START_FILE_PATH)) {
        final var controllerProperties = Stream.of(ClusterControllerProperty.values())
            .map(property -> Map.entry(property.getPropertyName(), property.get()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        System.setProperty(CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES.getPropertyName(), "false");
        System.setProperty(CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER.getPropertyName(), "false");
        System.setProperty(CLUSTER_CONTROLLER_RECONCILE_PATRONI.getPropertyName(), "false");
        System.setProperty(CLUSTER_CONTROLLER_RECONCILE_MANAGED_SQL.getPropertyName(), "false");

        runSingleReconciliationCycle();

        controllerProperties.entrySet().stream()
            .forEach(entry -> entry.getValue()
                .ifPresentOrElse(
                    value -> System.setProperty(entry.getKey(), value),
                    () -> System.clearProperty(entry.getKey())));
      }

      LOGGER.info("Starting StackGres Cluster Controller...");
      startupEvent.fire(StackGresClusterControllerAppStartupEvent.INSTANCE);
      Quarkus.waitForExit();
      shutdownEvent.fire(StackGresClusterControllerAppShutdownEvent.INSTANCE);
      return 0;
    }

    private static Boolean isReconciliationCycle(String... args) {
      return Seq.seq(Optional.ofNullable(System.getenv("COMMAND")))
          .append(Optional.ofNullable(args).map(Seq::of).orElse(Seq.empty()))
          .filter(command -> !command.isEmpty())
          .map(command -> command.equals("run-reconciliation-cycle"))
          .findFirst()
          .orElse(false);
    }

    private void runSingleReconciliationCycle() {
      LOGGER.info("Running StackGres Cluster Controller reconciliation cycle");
      List<StackGresCluster> existingContextResources =
          reconciliationCycle.getExistingContextResources();
      final ReconciliationCycleResult<?> result;
      if (existingContextResources.isEmpty()) {
        result = new ReconciliationCycleResult<>(
            new Exception("Not able to retrieve StackGres Cluster"));
      } else {
        result = reconciliationCycle.reconciliationCycle(existingContextResources
            .stream().map(Optional::of).toList());
      }
      if (!result.success()) {
        RuntimeException ex = Seq.seq(result.getException())
            .append(result.getContextExceptions().values().stream())
            .reduce(new RuntimeException("StackGres Cluster Controller"
                + " reconciliation cycle failed"),
                (exception, suppressedException) -> {
                  exception.addSuppressed(suppressedException);
                  return exception;
                },
                (u, v) -> v);
        if (!ClusterControllerReconciliationCycle.existsContextResource()) {
          throw ex;
        }
      }
    }
  }

  public enum StackGresClusterControllerAppStartupEvent {
    INSTANCE;
  }

  public enum StackGresClusterControllerAppShutdownEvent {
    INSTANCE;
  }
}
