/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.ongres.junit.docker.Container;

import io.stackgres.operator.app.StackGresOperatorApp;

import org.apache.commons.io.IOUtils;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItHelper {

  private final static Logger LOGGER = LoggerFactory.getLogger(ItHelper.class);

  public final static boolean OPERATOR_IN_KUBERNETES = Boolean.valueOf(System.getenv("OPERATOR_IN_KUBERNETES"));
  public final static String IMAGE_TAG = Optional.ofNullable(System.getenv("IMAGE_TAG"))
      .orElse("development-jvm");
  public final static Predicate<String> EXCLUDE_TTY_WARNING = line -> !line.equals("stdin: is not a tty");


  /**
   * IT helper method.
   */
  public static void copyResources(Container kind) throws Exception {
    kind.execute("rm", "-Rf", "/resources").forEach(line -> LOGGER.info(line));
    kind.copyResourcesIn("/stackgres-operator", StackGresOperatorApp.class, "/resources/stackgres-operator");
    kind.copyResourcesIn("/stackgres-cluster", StackGresOperatorApp.class, "/resources/stackgres-cluster");
  }

  /**
   * It helper method.
   */
  public static void restartKind(Container kind) throws Exception {
    LOGGER.info("Restarting kind");
    kind.execute("sh", "/scripts/restart-kind.sh")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void createNamespace(Container kind, String namespace) throws Exception {
    LOGGER.info("Create namespace '" + namespace + "'");
    kind.execute("sh", "-l", "-c",
        "kubectl create namespace " + namespace)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void deleteNamespaceIfExists(Container kind, String namespace) throws Exception {
    LOGGER.info("Deleting namespace if exists '" + namespace + "'");
    kind.execute("sh", "-l", "-c",
        "kubectl delete namespace --ignore-not-found " + namespace)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void deleteStackGresOperatorHelmChartIfExists(Container kind) throws Exception {
    LOGGER.info("Deleting if exists stackgres-operator helm chart");
    kind.execute("sh", "-l", "-c", "helm template /resources/stackgres-operator"
        + " --name stackgres-operator"
        + " --set-string cert.crt=undefined"
        + " --set-string cert.key=undefined"
        + " | kubectl delete --ignore-not-found -f -")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    kind.execute("sh", "-l", "-c", "helm delete stackgres-operator --purge || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void installStackGresOperatorHelmChart(Container kind, int sslPort,
      Executor executor) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      LOGGER.info("Loading stackgres operator image stackgres/operator:" + IMAGE_TAG);
      kind.execute("sh", "-l", "-c",
        "CONTAINER_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\"|cut -d '/' -f 2)\";"
        + "kind load docker-image --name \"$CONTAINER_NAME\""
          + " stackgres/operator:" + IMAGE_TAG)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
      LOGGER.info("Installing stackgres-operator helm chart");
      kind.execute("sh", "-l", "-c", "helm install /resources/stackgres-operator"
          + " --name stackgres-operator"
          + " --set-string image.tag=" + IMAGE_TAG
          + " --set-string image.pullPolicy=Never")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
      return;
    }

    LOGGER.info("Installing stackgres-operator helm chart");
    kind.execute("sh", "-l", "-c", "helm install /resources/stackgres-operator"
        + " --name stackgres-operator"
        + " --set deploy.create=false"
        + " --set-string cert.crt=" + Base64.getEncoder().encodeToString(
            IOUtils.toByteArray(ItHelper.class.getResourceAsStream("/certs/server.crt")))
        + " --set-string cert.key=" + Base64.getEncoder().encodeToString(
            IOUtils.toByteArray(ItHelper.class.getResourceAsStream("/certs/server-key.pem"))))
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
    Process process = new ProcessBuilder("sh", "-ec",
        "cat /proc/net/fib_trie | tr -d ' |-' | grep -F '172.17.0' | grep -v -F '172.17.0.0'")
        .start();
    CompletableFuture<String> dockerInterfaceIp = CompletableFuture.supplyAsync(
        Unchecked.supplier(() -> IOUtils.readLines(
            process.getInputStream(), StandardCharsets.UTF_8)
            .stream().findAny().get()), executor);
    CompletableFuture<List<String>> dockerInterfaceIpError = CompletableFuture.supplyAsync(
        Unchecked.supplier(() -> IOUtils.readLines(
            process.getErrorStream(), StandardCharsets.UTF_8)), executor);
    if (process.waitFor() != 0) {
      throw new RuntimeException("Can not retrieve docker interface IP:\n"
          + dockerInterfaceIpError.join().stream().collect(Collectors.joining("\n")));
    }
    kind.execute("sh", "-l", "-c", "cat << 'EOF' | kubectl create -f -\n"
        + "kind: Service\n"
        + "apiVersion: v1\n"
        + "metadata:\n"
        + "  namespace: stackgres\n"
        + "  name: stackgres-operator\n"
        + "spec:\n"
        + "  ports:\n"
        + "   - port: 443\n"
        + "     targetPort: " + sslPort + "\n"
        + "---\n"
        + "kind: Endpoints\n"
        + "apiVersion: v1\n"
        + "metadata:\n"
        + "  namespace: stackgres\n"
        + "  name: stackgres-operator\n"
        + "subsets:\n"
        + " - addresses:\n"
        + "    - ip: " + dockerInterfaceIp.join() + "\n"
        + "   ports:\n"
        + "    - port: " + sslPort + "\n"
        + "EOF")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void installStackGresConfigs(Container kind, String namespace) throws Exception {
    LOGGER.info("Deleting if exists stackgres-cluster helm chart for configs");
    kind.execute("sh", "-l", "-c", "helm delete stackgres-cluster-configs --purge || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    LOGGER.info("Installing stackgres-cluster helm chart for configs");
    kind.execute("sh", "-l", "-c", "helm install /resources/stackgres-cluster"
        + " --namespace " + namespace
        + " --name stackgres-cluster-configs"
        + " --set cluster.create=false"
        + " || true")
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void installStackGresCluster(Container kind, String namespace, String name, int instances) throws Exception {
    LOGGER.info("Deleting if exists stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("sh", "-l", "-c", "helm delete stackgres-cluster-" + name + " --purge || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    LOGGER.info("Installing stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("sh", "-l", "-c", "helm install /resources/stackgres-cluster"
        + " --namespace " + namespace
        + " --name stackgres-cluster-" + name
        + " --set config.create=false --set profiles.create=false"
        + " --set-string cluster.name=" + name
        + " --set cluster.instances=" + instances)
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void upgradeStackGresCluster(Container kind, String namespace, String name, int instances) throws Exception {
    LOGGER.info("Upgrade stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("sh", "-l", "-c", "helm upgrade stackgres-cluster-" + name
        + " /resources/stackgres-cluster"
        + " --set config.create=false --set profiles.create=false"
        + " --set-string cluster.name=" + name
        + " --set cluster.instances=" + instances)
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void deleteStackGresCluster(Container kind, String namespace, String name) throws Exception {
    LOGGER.info("Upgrade stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("sh", "-l", "-c", "helm delete stackgres-cluster-" + name)
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void waitUntilOperatorIsReady(CompletableFuture<Void> operator,
      WebTarget operatorClient, Container kind) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      waitUntil(Unchecked.supplier(() -> kind.execute("sh", "-l", "-c",
          "kubectl get pod -n stackgres"
              + " | grep 'stackgres-operator'"
              + " | grep -v 'stackgres-operator-init'"
              + " | cut -d ' ' -f 1"
              + " | xargs kubectl describe pod -n  stackgres ")),
          s -> s.anyMatch(line -> line.matches("  Ready\\s+True\\s*")), 120, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking availability of"
                  + " stackgres-operator pod:\n"
                  + s.collect(Collectors.joining("\n"))));
      return;
    }
    Instant timeout = Instant.now().plusSeconds(180);
    while (true) {
      if (Instant.now().isAfter(timeout)) {
        throw new TimeoutException();
      }
      TimeUnit.MILLISECONDS.sleep(100);
      try {
        if (operator.isDone()) {
          operator.join();
        }
        if (operatorClient.path("/health")
            .request(MediaType.APPLICATION_JSON)
            .get().getStatusInfo().equals(Status.OK)) {
          break;
        }
      } catch (ProcessingException ex) {
        if (ex.getCause() instanceof ConnectException) {
          continue;
        }
        throw ex;
      }
    }
  }


  /**
   * IT helper method.
   * Code has been copied and adapted from {@code QuarkusTestExtension} to allow start/stop
   * quarkus application inside a test.
   */
  public static OperatorRunner createOperator(Container kind, Class<?> testClass, int port,
      int sslPort, Executor executor) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      return new KubernetesOperatorRunner(kind, executor);
    }

    return new LocalOperatorRunner(kind, testClass, port, sslPort);
  }

  public static <T> void waitUntil(Supplier<T> supplier, Predicate<T> condition, int timeout,
      TemporalUnit unit, Consumer<T> onTimeout) throws Exception {
    Instant end = Instant.now().plus(Duration.of(timeout, unit));
    while (true) {
      if (Instant.now().isAfter(end)) {
        onTimeout.accept(supplier.get());
      }
      TimeUnit.SECONDS.sleep(1);
      try {
        if (condition.test(supplier.get())) {
          break;
        }
      } catch (Exception ex) {
        continue;
      }
    }
  }

  private ItHelper() {
  }
}
