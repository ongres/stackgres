/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.stackgres.cluster.controller.ClusterControllerReconciliationCycle;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle.ReconciliationCycleResult;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusMain
public class StackGresClusterControllerMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StackGresClusterControllerMain.class);

  public static void main(String... args) {
    if (isReconciliationCycle(args)) {
      AtomicReference<Tuple2<Integer, Throwable>> exitCodeReference =
          new AtomicReference<>(Tuple.tuple(0, null));
      Quarkus.run(StackGresClusterControllerReconcile.class,
          (exitCode, throwable) -> exitCodeReference.set(Tuple.tuple(exitCode, throwable)),
          args);
      if (exitCodeReference.get().v1 != 0) {
        throw new RuntimeException("exit code " + exitCodeReference.get(),
            exitCodeReference.get().v2);
      }
      return;
    }
    Quarkus.run(StackGresClusterControllerApp.class, args);
  }

  private static Boolean isReconciliationCycle(String... args) {
    return Seq.seq(Optional.ofNullable(System.getenv("COMMAND")))
        .append(Optional.ofNullable(args).map(Seq::of).orElse(Seq.empty()))
        .filter(command -> !command.isEmpty())
        .map(command -> command.equals("run-reconciliation-cycle"))
        .findFirst()
        .orElse(false);
  }

  public static class StackGresClusterControllerReconcile implements QuarkusApplication {
    private final ClusterControllerReconciliationCycle reconciliationCycle;

    @Inject
    public StackGresClusterControllerReconcile(
        ClusterControllerReconciliationCycle reconciliationCycle) {
      this.reconciliationCycle = reconciliationCycle;
    }

    @Override
    public int run(String... args) throws Exception {
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
        if (ex.getSuppressed().length > 0) {
          throw ex;
        }
      }
      return 0;
    }

  }

  public static class StackGresClusterControllerApp implements QuarkusApplication {

    private final Event<StackGresClusterControllerAppStartupEvent> startupEvent;
    private final Event<StackGresClusterControllerAppShutdownEvent> shutdownEvent;

    @Inject
    public StackGresClusterControllerApp(
        Event<StackGresClusterControllerAppStartupEvent> startupEvent,
        Event<StackGresClusterControllerAppShutdownEvent> shutdownEvent) {
      this.startupEvent = startupEvent;
      this.shutdownEvent = shutdownEvent;
    }

    @Override
    public int run(String... args) throws Exception {
      LOGGER.info("Starting StackGres Cluster Controller...");
      startupEvent.fire(StackGresClusterControllerAppStartupEvent.INSTANCE);
      Quarkus.waitForExit();
      shutdownEvent.fire(StackGresClusterControllerAppShutdownEvent.INSTANCE);
      return 0;
    }
  }

  public enum StackGresClusterControllerAppStartupEvent {
    INSTANCE;
  }

  public enum StackGresClusterControllerAppShutdownEvent {
    INSTANCE;
  }
}
