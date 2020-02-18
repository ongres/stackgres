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

  private final static Logger LOGGER = LoggerFactory.getLogger(KubernetesOperatorRunner.class);

  private final CompletableFuture<Void> future = new CompletableFuture<Void>();

  private final Container k8s;
  private final Executor executor;

  public KubernetesOperatorRunner(Container k8s, Executor executor) {
    super();
    this.k8s = k8s;
    this.executor = executor;
  }

  @Override
  public void close() throws IOException {
    future.complete(null);
  }

  @Override
  public void run() throws Throwable {
    ItHelper.waitUntilOperatorIsReady(future, null, k8s);
    CompletableFuture<Void> runnerLogFuture = CompletableFuture.runAsync(() -> {
      try {
        k8s.execute("sh", "-l", "-c",
            " kubectl get pod -n stackgres"
                + " | grep 'stackgres-operator'"
                + " | grep -v 'stackgres-operator-init'"
                + " | cut -d ' ' -f 1"
                + " | xargs kubectl logs -n stackgres -c stackgres-operator -f ")
            .filter(ItHelper.EXCLUDE_TTY_WARNING)
            .forEach(line -> LOGGER.info(line));
      } catch (Exception ex) {
        return;
      }
    }, executor);
    future.join();
    CompletableFuture<Void> runnerLogKillerStopper = new CompletableFuture<>();
    CompletableFuture<Void> runnerLogKiller = CompletableFuture.runAsync(Unchecked.runnable(() -> {
      while (!runnerLogKillerStopper.isDone()) {
        k8s.execute("sh", "-l", "-c",
            "ps | grep ' kubectl logs ' | grep -v ' grep '"
                + " | grep -v ' xargs kubectl logs '"
                + " | sed 's/\\s\\+/ /g' | sed 's/^ //'"
                + " | cut -d ' ' -f 1 | xargs -r kill")
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
