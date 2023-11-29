/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.ongres.junit.docker.Container;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesOperatorRunner implements OperatorRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesOperatorRunner.class);

  private final CompletableFuture<Void> future = new CompletableFuture<Void>();

  private final Container k8s;
  private final String namespace;
  private final Executor executor;

  public KubernetesOperatorRunner(Container k8s, String namespace, Executor executor) {
    super();
    this.k8s = k8s;
    this.namespace = namespace;
    this.executor = executor;
  }

  @Override
  public void close() throws IOException {
    future.complete(null);
  }

  @Override
  public void run() throws Throwable {
    ItHelper.waitUntilOperatorIsReady(future, null, k8s, namespace);
    CompletableFuture<Void> runnerLogFuture = CompletableFuture.runAsync(() -> {
      try {
        k8s.execute("sh", "-l", "-c",
            "while kubectl get pod -n " + namespace
                + " -l app=stackgres-operator -o name; do sleep 1; done"
                + " | cut -d '/' -f 2"
                + " | (while read POD\n"
                + "  do"
                + "    if echo $PODS | grep -q :$POD:\n"
                + "    then\n"
                + "      continue\n"
                + "    fi\n"
                + "    PODS=$PODS:$POD:\n"
                + "    echo $POD\n"
                + " done)"
                + " | xargs -r -n 1 kubectl logs -n " + namespace
                + " -c stackgres-operator -f || true")
            .filter(ItHelper.EXCLUDE_TTY_WARNING)
            .forEach(line -> LOGGER.info(line));
      } catch (Exception ex) {
        LOGGER.trace("", ex);
        return;
      }
    }, executor);
    future.join();
    CompletableFuture<Void> runnerLogKillerStopper = new CompletableFuture<>();
    CompletableFuture<Void> runnerLogKiller = CompletableFuture.runAsync(Unchecked.runnable(() -> {
      while (!runnerLogKillerStopper.isDone()) {
        k8s.execute("sh", "-l", "-c",
            "ps -e -o pid,args"
                + " | grep -v ' xargs [k]ubectl logs '"
                + " | grep ' [k]ubectl logs '"
                + " | (while read PID ARGS; do echo $PID; done)"
                + " | xargs -r kill || true")
            .filter(ItHelper.EXCLUDE_TTY_WARNING)
            .forEach(line -> LOGGER.info(line));
        TimeUnit.SECONDS.sleep(1);
      }
    }), executor);
    runnerLogFuture.join();
    runnerLogKillerStopper.complete(null);
    runnerLogKiller.join();
  }
}
