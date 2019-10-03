/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.net.ConnectException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.ongres.junit.docker.Container;

import io.stackgres.operator.app.StackGresOperatorApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItHelper {

  private final static Logger LOGGER = LoggerFactory.getLogger(ItHelper.class);

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
    kind.execute("bash", "/scripts/restart-kind.sh")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void createNamespace(Container kind, String namespace) throws Exception {
    LOGGER.info("Create namespace '" + namespace + "'");
    kind.execute("bash", "-l", "-c",
        "kubectl create namespace " + namespace)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void deleteNamespaceIfExists(Container kind, String namespace) throws Exception {
    LOGGER.info("Deleting namespace if exists '" + namespace + "'");
    kind.execute("bash", "-l", "-c",
        "kubectl delete namespace --ignore-not-found " + namespace)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void deleteStackGresOperatorHelmChartIfExists(Container kind) throws Exception {
    LOGGER.info("Deleting if exists stackgres-operator helm chart");
    kind.execute("bash", "-l", "-c", "helm template /resources/stackgres-operator"
        + " --name stackgres-operator"
        + " | kubectl delete --ignore-not-found -f -")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    kind.execute("bash", "-l", "-c", "helm delete stackgres-operator --purge || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void installStackGresOperatorHelmChart(Container kind, int sslPort)
      throws Exception {
    LOGGER.info("Installing stackgres-operator helm chart");
    kind.execute("bash", "-l", "-c", "helm install /resources/stackgres-operator"
        + " --name stackgres-operator"
        + " --set deploy.create=false"
        + " --set service.create=false")
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
    kind.execute("bash", "-l", "-c", "cat << 'EOF' | kubectl create -f -\n"
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
        + "    - ip: 172.17.0.1\n"
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
    try{

      kind.execute("bash", "-l", "-c", "helm delete stackgres-cluster-configs --purge || true")
          .filter(EXCLUDE_TTY_WARNING)
          .forEach(line -> LOGGER.info(line));
    } catch (Exception ex){
      LOGGER.error("Error deleting existent cluster", ex);
      throw ex;
    }
    try {
      LOGGER.info("Installing stackgres-cluster helm chart for configs");
      kind.execute("bash", "-l", "-c", "helm install /resources/stackgres-cluster"
          + " --namespace " + namespace
          + " --name stackgres-cluster-configs"
          + " --set cluster.create=false")
          .filter(EXCLUDE_TTY_WARNING)
          .forEach(line -> LOGGER.info(line));
    } catch (Exception ex){
      LOGGER.error("Error installing cluster in namespace " + namespace, ex);
      throw ex;
    }
  }

  /**
   * It helper method.
   */
  public static void installStackGresCluster(Container kind, String namespace, String name) throws Exception {
    LOGGER.info("Deleting if exists stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("bash", "-l", "-c", "helm delete stackgres-cluster-" + name + " --purge || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    LOGGER.info("Installing stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("bash", "-l", "-c", "helm install /resources/stackgres-cluster"
        + " --namespace " + namespace
        + " --name stackgres-cluster-" + name
        + " --set config.create=false --set profiles.create=false"
        + " --set-string cluster.name=" + name)
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void waitUntilOperatorIsReady(CompletableFuture<Void> operator,
      WebTarget operatorClient) throws Exception {
    Instant timeout = Instant.now().plusSeconds(30);
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
  public static OperatorRunner createOperator(Class<?> testClass, Container kind, int port,
      int sslPort) throws Exception {
    return new OperatorRunner(testClass, kind, port, sslPort);
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
