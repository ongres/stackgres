/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.spotify.docker.client.exceptions.DockerException;

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
    Path helmChartPath = Paths.get("../..");
    kind.copyIn(helmChartPath.resolve("install/helm/stackgres-operator"),
        "/resources/stackgres-operator");
    kind.copyIn(helmChartPath.resolve("install/helm/stackgres-cluster"),
        "/resources/stackgres-cluster");
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
        "kubectl delete namespace --ignore-not-found " + namespace + " || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    waitUntil(
        Unchecked.supplier(() -> kind.execute("sh", "-l", "-c",
            "kubectl get namespace " + namespace
              + " --ignore-not-found --template '{{ .metadata.name }}'")
            .filter(EXCLUDE_TTY_WARNING)),
        lines -> lines.count() == 0,
        300, ChronoUnit.SECONDS,
        Unchecked.runnable(() -> kind.execute("sh", "-l", "-c",
            "kubectl describe namespace " + namespace + " || true")
            .filter(EXCLUDE_TTY_WARNING)
            .forEach(line -> LOGGER.info(line))));
  }

  /**
   * It helper method.
   */
  public static void deleteStackGresOperatorHelmChartIfExists(Container kind, String namespace)
      throws Exception {
    LOGGER.info("Deleting if exists stackgres-operator helm chart");
    kind.execute("sh", "-l", "-c", "helm template /resources/stackgres-operator"
        + " --namespace " + namespace
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
  public static void installStackGresOperatorHelmChart(Container kind, String namespace,
      int sslPort, Executor executor) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      LOGGER.info("Loading stackgres operator image stackgres/operator:" + IMAGE_TAG);
      kind.execute("sh", "-l", "-c",
        "KIND_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\"|cut -d '/' -f 2)\";"
        + "kind load docker-image --name \"$KIND_NAME\""
          + " stackgres/operator:" + IMAGE_TAG)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
      LOGGER.info("Installing stackgres-operator helm chart");
      kind.execute("sh", "-l", "-c", "helm install /resources/stackgres-operator"
          + " --namespace " + namespace
          + " --name stackgres-operator"
          + " --set-string image.tag=" + IMAGE_TAG
          + " --set-string image.pullPolicy=Never")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
      return;
    }

    LOGGER.info("Installing stackgres-operator helm chart");
    kind.execute("sh", "-l", "-c", "helm install /resources/stackgres-operator"
        + " --namespace stackgres"
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

  public static void installMinioHelmChart(Container kind, String namespace, String clusterNamespace)
      throws DockerException, InterruptedException {
    LOGGER.info("Installing minio helm chart");
    kind.execute("sh", "-l", "-c", "helm upgrade minio stable/minio"
        + " --install --version 2.5.18 --namespace " + namespace
        + " --set buckets[0].name=stackgres,buckets[0].policy=none,buckets[0].purge=true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    kind.execute("sh", "-l", "-c", "kubectl get secret -n minio minio -o yaml"
        + " | sed 's/  namespace: " + namespace + "/  namespace: " + clusterNamespace + "/'"
        + " | kubectl create --namespace " + clusterNamespace + " -f -")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    kind.execute("sh", "-l", "-c", "cat << 'EOF' | kubectl create -f -\n"
        + "kind: Service\n"
        + "apiVersion: v1\n"
        + "metadata:\n"
        + "  namespace: " + clusterNamespace + "\n"
        + "  name: minio\n"
        + "spec:\n"
        + "  type: ExternalName\n"
        + "  externalName: minio." + namespace + ".svc.cluster.local\n"
        + "  ports:\n"
        + "   - port: 9000\n"
        + "EOF")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void installStackGresConfigs(Container kind, String namespace, boolean withMinio)
      throws Exception {
    LOGGER.info("Deleting if exists stackgres-cluster helm chart for configs");
    kind.execute("sh", "-l", "-c", "helm delete stackgres-cluster-configs --purge || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    LOGGER.info("Installing stackgres-cluster helm chart for configs");
    kind.execute("sh", "-l", "-c", "helm install /resources/stackgres-cluster"
        + " --namespace " + namespace
        + " --name stackgres-cluster-configs"
        + " --set cluster.create=false"
        + getMinioOptions(withMinio, namespace))
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void installStackGresCluster(Container kind, String namespace, String name,
      int instances, boolean withMinio) throws Exception {
    LOGGER.info("Deleting if exists stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("sh", "-l", "-c", "helm delete stackgres-cluster-" + name + " --purge || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    LOGGER.info("Deleting if exists stackgres-cluster resources for cluster with name " + name);
    kind.execute("sh", "-l", "-c", "kubectl delete statefulset"
        + " -n " + namespace + " " + name + " --ignore-not-found")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    LOGGER.info("Installing stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("sh", "-l", "-c", "helm install /resources/stackgres-cluster"
        + " --namespace " + namespace
        + " --name stackgres-cluster-" + name
        + " --set config.create=false --set profiles.create=false"
        + " --set-string cluster.name=" + name
        + " --set cluster.instances=" + instances
        + getMinioOptions(withMinio, namespace))
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  private static String getMinioOptions(boolean withMinio, String namespace) {
    return !withMinio ? "" :
      " --set cluster.backup.retention=5"
      + " --set-string cluster.backup.fullSchedule='*/1 * * * *'"
      + " --set cluster.backup.fullWindow=1"
      + " --set-string cluster.backup.s3.prefix=s3://stackgres"
      + " --set-string cluster.backup.s3.endpoint=http://minio." + namespace + ".svc:9000"
      + " --set cluster.backup.s3.forcePathStyle=true"
      + " --set-string  cluster.backup.s3.region=k8s"
      + " --set-string cluster.backup.s3.accessKey.name=minio"
      + " --set-string cluster.backup.s3.accessKey.key=accesskey"
      + " --set-string cluster.backup.s3.secretKey.name=minio"
      + " --set-string cluster.backup.s3.secretKey.key=secretkey";
  }

  /**
   * It helper method.
   */
  public static void upgradeStackGresCluster(Container kind, String namespace, String name,
      int instances) throws Exception {
    LOGGER.info("Upgrade stackgres-cluster helm chart for cluster with name " + name);
    kind.execute("sh", "-l", "-c", "helm upgrade stackgres-cluster-" + name
        + " /resources/stackgres-cluster --reuse-values"
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
    LOGGER.info("Delete stackgres-cluster helm chart for cluster with name " + name);
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
          "kubectl get pod -n stackgres -o name"
              + " | grep '^pod/stackgres-operator-'"
              + " | grep -v '^pod/stackgres-operator-init'"
              + " | xargs kubectl describe -n  stackgres ")),
          s -> s.anyMatch(line -> line.matches("^  Ready\\s+True\\s*")), 120, ChronoUnit.SECONDS,
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
    waitUntil(supplier, condition, timeout, unit, () -> onTimeout.accept(supplier.get()));
  }

  public static <T> void waitUntil(Supplier<T> supplier, Predicate<T> condition, int timeout,
      TemporalUnit unit, Runnable onTimeout) throws Exception {
    Instant end = Instant.now().plus(Duration.of(timeout, unit));
    while (true) {
      if (Instant.now().isAfter(end)) {
        onTimeout.run();
        throw new TimeoutException("Timeout after waiting for the specified condition for "
            + Duration.of(timeout, unit).getSeconds() + " seconds");
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
