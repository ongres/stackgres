package io.stackgres.operator;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.ongres.junit.docker.Container;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesOperatorRunner implements OperatorRunner {

  private final static Logger LOGGER = LoggerFactory.getLogger(KubernetesOperatorRunner.class);

  private final CompletableFuture<Void> future = new CompletableFuture<Void>();

  private final Container kind;
  private final Executor executor;

  public KubernetesOperatorRunner(Container kind, Executor executor) {
    super();
    this.kind = kind;
    this.executor = executor;
  }

  @Override
  public void close() throws IOException {
    future.complete(null);
  }

  @Override
  public void run() throws Throwable {
    ItHelper.waitUntilOperatorIsReady(future, null, kind);
    CompletableFuture<String> runnerLogPid = new CompletableFuture<String>();
    CompletableFuture<Void> runnerLogFuture = CompletableFuture.runAsync(() -> {
      try {
        kind.execute("sh", "-l", "-c",
            "echo $$;"
                + " trap 'kill $(jobs -p)' EXIT;"
                + " kubectl get pod -n stackgres"
                + " | grep 'stackgres-operator'"
                + " | grep -v 'stackgres-operator-init'"
                + " | cut -d ' ' -f 1"
                + " | xargs kubectl logs -n stackgres -c stackgres-operator -f ")
            .filter(ItHelper.EXCLUDE_TTY_WARNING)
            .peek(line -> {
              if (!runnerLogPid.isDone()) {
                runnerLogPid.complete(line);
              }
            })
            .forEach(line -> LOGGER.info(line));
      } catch (Exception ex) {
        return;
      }
    }, executor);
    future.join();
    CompletableFuture.runAsync(Unchecked.runnable(() -> {
      kind.execute("sh", "-l", "-c",
          "kill " + runnerLogPid.join())
          .filter(ItHelper.EXCLUDE_TTY_WARNING)
          .forEach(line -> LOGGER.info(line));
    }), executor);
     runnerLogFuture.join();
  }
}
