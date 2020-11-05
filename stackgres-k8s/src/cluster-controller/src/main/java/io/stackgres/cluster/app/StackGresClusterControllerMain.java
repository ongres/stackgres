/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.stackgres.cluster.controller.ClusterControllerReconciliationCycle;
import io.stackgres.operatorframework.reconciliation.ReconciliationCycle.ReconciliationCycleResult;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusMain
public class StackGresClusterControllerMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StackGresClusterControllerMain.class);

  public static void main(String... args) {
    if (Seq.seq(Optional.ofNullable(System.getenv("COMMAND")))
        .append(Optional.ofNullable(args).map(Seq::of).orElse(Seq.empty()))
        .filter(command -> !command.isEmpty())
        .map(command -> command.equals("run-reconciliation-cycle"))
        .findFirst()
        .orElse(false)) {
      AtomicInteger exitCodeReference = new AtomicInteger(0);
      Quarkus.run(StackGresClusterControllerReconcile.class,
          (exitCode, throwable) -> exitCodeReference.set(exitCode),
          args);
      if (exitCodeReference.get() != 0) {
        throw new RuntimeException("exit code " + exitCodeReference.get());
      }
      return;
    }
    Quarkus.run(StackGresClusterControllerApp.class, args);
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
      ReconciliationCycleResult<?> result = reconciliationCycle.reconciliationCycle();
      if (!result.success()) {
        throw Seq.seq(result.getException())
            .append(result.getContextExceptions().values().stream())
            .reduce(new RuntimeException("StackGres Cluster Controller"
                + " reconciliation cycle failed"),
                (exception, suppressedException) -> {
                  exception.addSuppressed(suppressedException);
                  return exception;
                },
                (u, v) -> v);
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
