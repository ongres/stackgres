package io.stackgres.operator;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import com.ongres.junit.docker.Container;

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
    final Stream<String> runnerLogStream = kind.execute("sh", "-l", "-c",
        "(sleep 120; while true; do sleep 5; seq -s ' ' 10000000 10000910; done) &"
            + " kubectl get pod -n stackgres"
            + " | grep 'stackgres-operator'"
            + " | grep -v 'stackgres-operator-init'"
            + " | cut -d ' ' -f 1"
            + " | xargs kubectl logs -n stackgres -c stackgres-operator -f ")
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .filter(line -> !line.startsWith("10000000 10000001 10000002 "));
    CompletableFuture.runAsync(() -> {
      try {
        runnerLogStream.forEach(line -> LOGGER.info(line));
      } catch (Exception ex) {
        LOGGER.warn("An error occurred while logging operator", ex);
      }
    }, executor);
    future.join();
    runnerLogStream.close();
  }
}
