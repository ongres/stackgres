/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.ByteArrayInputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Base64;
import java.util.Locale;
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

import com.github.dockerjava.api.exception.DockerException;
import com.google.common.collect.ImmutableList;
import com.ongres.junit.docker.Container;
import org.apache.commons.io.IOUtils;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItHelper {

  private final static Logger LOGGER = LoggerFactory.getLogger("ItHelper");

  public final static boolean OPERATOR_IN_KUBERNETES = Boolean.valueOf(System.getenv("OPERATOR_IN_KUBERNETES"));
  public final static String IMAGE_TAG = Optional.ofNullable(System.getenv("IMAGE_TAG"))
      .orElse("development-jvm");
  public final static Predicate<String> EXCLUDE_TTY_WARNING = line -> !line.equals("stdin: is not a tty");

  public static final String E2E_ENV_VAR_NAME = Optional.ofNullable(System.getenv("E2E_ENV"))
      .map(env -> env.toUpperCase(Locale.US))
      .map(env -> env + "_NAME")
      .orElse("KIND_NAME");

  public static final String E2E_ENV = Optional.ofNullable(System.getenv("E2E_ENV"))
      .orElse("kind");

  public static final String E2E_ENVVARS = Seq.seq(System.getenv().entrySet())
      .filter(e -> e.getKey().startsWith("E2E_") || e.getKey().startsWith("K8S_")
          || ImmutableList.of("IMAGE_TAG", "IMAGE_NAME").contains(e.getKey()))
      .map(e -> "export " + e.getKey() + "=\"" + e.getValue() + "\"")
      .collect(Collectors.joining("\n"));

  public static final boolean E2E_DEBUG = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_DEBUG"))
      .orElse(System.getProperty("e2e.debug")))
      .map(Boolean::valueOf)
      .orElse(false);

  /**
   * IT helper method.
   */
  public static void killUnwantedProcesses(Container k8s) throws Exception {
    k8s.execute("sh", "-c",
        "#!/bin/sh\n"
            + "TIMEOUT=20\n"
            + "START=\"$(date +%s)\"\n"
            + "while [ \"$((START + TIMEOUT))\" -gt \"$(date +%s)\" ] \\\n"
            + "  && ! ps -o pid,ppid,args | sed 's/^ \\+//' | sed 's/ \\+/ /g' \\\n"
            + "  | grep -v '^\\([0-9]\\+ \\)\\?\\(1\\|'\"$$\"'\\) ' \\\n"
            + "  | cut -d ' ' -f 1 | tail -n +2 \\\n"
            + "  | wc -l | grep -q '^0$'\n"
            + "do\n"
            + "  echo 'Killing following unwanted processes:'\n"
            + "  ps -o pid,ppid,args | sed 's/^ \\+//' | sed 's/ \\+/ /g' \\\n"
            + "    | grep -v '^\\([0-9]\\+ \\)\\?\\(1\\|'\"$$\"'\\) '\n"
            + "  ps -o pid,ppid,args | sed 's/^ \\+//' | sed 's/ \\+/ /g' \\\n"
            + "    | grep -v '^\\([0-9]\\+ \\)\\?\\(1\\|'\"$$\"'\\) ' \\\n"
            + "    | cut -d ' ' -f 1 | tail -n +2 \\\n"
            + "    | xargs -r -n 1 -I % kill % >/dev/null 2>&1 || true\n"
            + "done\n"
            + "if [ \"$((START + TIMEOUT))\" -le \"$(date +%s)\" ]\n"
            + "then\n"
            + "  echo 'Timeout while trying to kill unwanted processes'\n"
            + "  exit 1\n"
            + "fi\n")
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * IT helper method.
   */
  public static void copyResources(Container k8s) throws Exception {
    k8s.execute("rm", "-Rf", "/resources").forEach(line -> LOGGER.info(line));
    Path k8sPath = Paths.get("../..");
    k8s.copyIn(k8sPath.resolve("install/helm/stackgres-operator"),
        "/resources/stackgres-operator");
    k8s.copyIn(k8sPath.resolve("install/helm/stackgres-cluster"),
        "/resources/stackgres-cluster");
    k8s.copyIn(k8sPath.resolve("e2e"), "/resources/e2e");
    k8s.copyIn(Paths.get("src/test/resources/certs"), "/resources/certs");
  }

  /**
   * It helper method.
   */
  public static void resetKind(Container k8s, int size) throws Exception {
    if (Optional.ofNullable(System.getenv("K8S_REUSE"))
        .map(Boolean::parseBoolean)
        .orElse(true)) {
      LOGGER.info("Reusing " + E2E_ENV);
      k8s.copyIn(new ByteArrayInputStream(
              ("cd /resources/e2e\n"
              + E2E_ENVVARS + "\n"
              + "export DOCKER_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\"|cut -d '/' -f 2)\"\n"
              + "export " + E2E_ENV_VAR_NAME + "="
                  + "\"" + E2E_ENV + "$(echo \"$DOCKER_NAME\" | sed 's/^k8s//')\"\n"
              + "export K8S_FROM_DIND=true\n"
              + "export IMAGE_TAG=" + ItHelper.IMAGE_TAG + "\n"
              + "export CLUSTER_CHART_PATH=/resources/stackgres-cluster\n"
              + "export OPERATOR_CHART_PATH=/resources/stackgres-operator\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e reuse_k8s\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e setup_helm\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e setup_default_limits 0.1 0.1 16Mi 16Mi\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e k8s_webhook_cleanup\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e helm_cleanup\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e k8s_cleanup\n"
              + (OPERATOR_IN_KUBERNETES
                  ? "sh " + (E2E_DEBUG ? "-x" : "") + " e2e load_operator_k8s\n" : "")
              + (OPERATOR_IN_KUBERNETES ? ""
                  : "sh " + (E2E_DEBUG ? "-x" : "")
                  + " e2e load_certificate_k8s /resources/certs/server.crt\n"))
              .getBytes(StandardCharsets.UTF_8)), "/reuse-k8s.sh");
      k8s.execute("sh", "-e", "/reuse-k8s.sh")
          .filter(ItHelper.EXCLUDE_TTY_WARNING)
          .forEach(LOGGER::info);
      return;
    }
    LOGGER.info("Restarting " + E2E_ENV);
    k8s.copyIn(new ByteArrayInputStream(
        ("cd /resources/e2e\n"
        + E2E_ENVVARS + "\n"
        + "export DOCKER_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\"|cut -d '/' -f 2)\"\n"
        + "export " + E2E_ENV_VAR_NAME + "="
            + "\"" + E2E_ENV + "$(echo \"$DOCKER_NAME\" | sed 's/^k8s//')\"\n"
        + "export K8S_FROM_DIND=true\n"
        + "export IMAGE_TAG=" + ItHelper.IMAGE_TAG + "\n"
        + "export CLUSTER_CHART_PATH=/resources/stackgres-cluster\n"
        + "export OPERATOR_CHART_PATH=/resources/stackgres-operator\n"
        + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e reset_k8s\n"
        + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e setup_helm\n"
        + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e setup_default_limits 0.1 0.1 16Mi 16Mi\n"
        + (OPERATOR_IN_KUBERNETES
            ? "sh " + (E2E_DEBUG ? "-x" : "") + " e2e load_operator_k8s\n" : "")
        + (OPERATOR_IN_KUBERNETES ? ""
            : "sh " + (E2E_DEBUG ? "-x" : "")
            + " e2e load_certificate_k8s /resources/certs/server.crt\n"))
        .getBytes(StandardCharsets.UTF_8)), "/restart-k8s.sh");
    k8s.execute("sh", "-e", "/restart-k8s.sh")
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

  /**
   * It helper method.
   */
  public static void createNamespace(Container k8s, String namespace) throws Exception {
    LOGGER.info("Create namespace '" + namespace + "'");
    k8s.execute("sh", "-l", "-c",
        "kubectl create namespace " + namespace)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static String createExposedHost(Container k8s, String host, int port)
      throws Exception {
    Optional<Integer> firstPointIndex = Optional.of(host.indexOf('.'))
        .filter(index -> index >= 0);
    Optional<Integer> secondPointIndex = firstPointIndex
        .map(index -> host.indexOf('.', index + 1))
        .filter(index -> index >= 0);
    if (!firstPointIndex.isPresent()) {
      return host;
    }
    String namespace = host.substring(firstPointIndex.get() + 1,
        secondPointIndex.orElse(host.length()));
    String serviceName = host.substring(0, firstPointIndex.get());
    String exposedServiceName = serviceName + "-exposed";
    LOGGER.info("Create service " + exposedServiceName
        + " to expose service " + serviceName + " in namespace " + namespace);
    k8s.execute("sh", "-l", "-ce",
        "kubectl get service -n " + namespace + " " + exposedServiceName
            + " | grep -q '^" + exposedServiceName + "\\s'"
            + " || kubectl expose service -n " + namespace + " " + serviceName
            + " --name " + exposedServiceName + " --type NodePort --labels app=StackGresMock;"
            + "NAME=\"$(kubectl get service -n " + namespace + " " + serviceName
            + " -o jsonpath='{ .spec.ports[?(@.port==" + port + ")].name }')\";"
            + "PORT=\"$(kubectl get service -n " + namespace + " " + serviceName
            + " -o jsonpath='{ .spec.ports[?(@.port==" + port + ")].port }')\";"
            + "PROTOCOL=\"$(kubectl get service -n " + namespace + " " + serviceName
            + " -o jsonpath='{ .spec.ports[?(@.port==" + port + ")].protocol }')\";"
            + "TARGET_PORT=\"$(kubectl get service -n " + namespace + " " + serviceName
            + " -o jsonpath='{ .spec.ports[?(@.port==" + port + ")].targetPort }')\";"
            + "kubectl patch service -n " + namespace + " " + exposedServiceName
            + " --type json --patch '[{\"op\":\"replace\",\"path\":\"/spec/ports\",\"value\":[{"
            + "\"name\":\"'\"$NAME\"'\",\"port\":'\"$PORT\"',\"protocol\":\"'\"$PROTOCOL\"'\","
            + "\"targetPort\":'\"$([ \"$TARGET_PORT\" -eq \"$TARGET_PORT\" ] 2> /dev/null"
            + " && echo \"$TARGET_PORT\" || echo \"\\\"$TARGET_PORT\\\"\")\"'}]}]';"
            + "END=\"$(($(date +%s)+10))\";"
            + "until kubectl get service -n " + namespace + " " + exposedServiceName
            + " -o jsonpath='{ .spec.ports[?(@.port==" + port + ")].nodePort }';"
            + " do [ \"$(date +%s)\" -lt \"$END\" ]; sleep 1; done;"
            )
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    return getKubernetesMasterIp(k8s) + ":" + k8s.execute("sh", "-l", "-c",
        "kubectl get service -n " + namespace + " " + exposedServiceName
            + " -o jsonpath='{ .spec.ports[?(@.port==" + port + ")].nodePort }'")
        .filter(EXCLUDE_TTY_WARNING)
        .collect(Collectors.joining());
  }

  /**
   * It helper method.
   */
  public static void installStackGresOperatorHelmChart(Container k8s, String namespace,
      int port) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      LOGGER.info("Installing stackgres-operator helm chart");
      k8s.execute("sh", "-l", "-c", "kubectl create namespace " + namespace + " || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
      k8s.execute("sh", "-l", "-c", "helm upgrade --install"
          + " stackgres-operator"
          + " --namespace " + namespace
          + " /resources/stackgres-operator"
          + " --set-string image.tag=" + IMAGE_TAG
          + " --set-string image.pullPolicy=Never"
          + " --set-string authentication.user=e2e"
          + " --set-string authentication.password=test")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
      return;
    }

    LOGGER.info("Installing stackgres-operator helm chart without operator container");
    k8s.execute("sh", "-l", "-c", "kubectl create namespace " + namespace + " || true")
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(LOGGER::info);
    k8s.execute("sh", "-l", "-c", "helm upgrade --install"
        + " stackgres-operator"
        + " --namespace stackgres"
        + " /resources/stackgres-operator"
        + getOperatorExtraOptions(k8s, port)
        + " --set-string authentication.user=e2e"
        + " --set-string authentication.password=test")
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  public static String getOperatorExtraOptions(Container k8s, int port) throws Exception {
    String dockerInterfaceIp = getDockerInterfaceIp(k8s);
    return " --set deploy.create=false"
        + " --set-string developer.externalOperatorIp=" + dockerInterfaceIp
        + " --set developer.externalOperatorPort=" + port
        + " --set-string cert.crt=" + Base64.getEncoder().encodeToString(
            IOUtils.toByteArray(ItHelper.class.getResourceAsStream("/certs/server.crt")))
        + " --set-string cert.key=" + Base64.getEncoder().encodeToString(
            IOUtils.toByteArray(ItHelper.class.getResourceAsStream("/certs/server-key.pem")));
  }

  public static String getDockerInterfaceIp(Container k8s)
      throws DockerException, InterruptedException {
    return k8s.execute("sh", "-l", "-c",
        "docker network inspect bridge -f '{{ (index .IPAM.Config 0).Gateway }}'")
    .filter(EXCLUDE_TTY_WARNING)
    .collect(Collectors.joining());
  }

  public static String getKubernetesMasterIp(Container k8s)
      throws DockerException, InterruptedException {
    return k8s.execute("sh", "-l", "-c",
        "kubectl cluster-info|grep 'Kubernetes master' | cut -d / -f  3 | cut -d : -f 1")
    .filter(EXCLUDE_TTY_WARNING)
    .collect(Collectors.joining());
  }

  /**
   * It helper method.
   */
  public static void installStackGresConfigs(Container k8s, String namespace)
      throws Exception {
    LOGGER.info("Deleting if exists stackgres-cluster helm chart for configs");
    k8s.execute("sh", "-l", "-c", "helm delete stackgres-cluster-configs --namespace "
        + namespace + "|| true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
    LOGGER.info("Installing stackgres-cluster helm chart for configs");
    k8s.execute("sh", "-l", "-c", "kubectl create namespace " + namespace)
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(LOGGER::info);
    k8s.execute("sh", "-l", "-c", "helm install"
        + " stackgres-cluster-configs"
        + " --namespace " + namespace
        + " /resources/stackgres-cluster"
        + " --set-string configurations.postgresconfig.postgresql\\.conf.shared_buffers=32MB"
        + " --set cluster.create=false"
        + " --set configurations.backupconfig.baseBackups.retention=5"
        + " --set-string configurations.backupconfig.baseBackups.cronSchedule='*/1 * * * *'"
        + " --set-string minio.persistence.size=128Mi")
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(LOGGER::info);
  }

  /**
   * It helper method.
   */
  public static void installStackGresCluster(Container k8s, String namespace, String name,
      int instances) throws Exception {
    LOGGER.info("Deleting if exists stackgres-cluster helm chart for cluster with name " + name);
    k8s.execute("sh", "-l", "-c", "helm delete " + name + " --namespace " + namespace + "|| true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
    LOGGER.info("Installing stackgres-cluster helm chart for cluster with name " + name);
    k8s.execute("sh", "-l", "-c", "helm install "
        + name
        + " --namespace " + namespace
        + " /resources/stackgres-cluster"
        + " --set configurations.create=false"
        + " --set instanceProfiles=null"
        + " --set-string cluster.name=" + name
        + " --set cluster.instances=" + instances
        + " --set-string cluster.pods.persistentVolume.size=128Mi"
        + " --set nonProductionOptions.createMinio=false")
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void upgradeStackGresCluster(Container k8s, String namespace, String name,
      int instances) throws Exception {
    LOGGER.info("Upgrade stackgres-cluster helm chart for cluster with name " + name);
    k8s.execute("sh", "-l", "-c", "helm upgrade "
        + name
        + " /resources/stackgres-cluster "
        + " --namespace " + namespace
        + " --set configurations.create=false"
        + " --set instanceProfiles=null"
        + " --set-string cluster.name=" + name
        + " --set cluster.instances=" + instances
        + " --set-string cluster.pods.persistentVolume.size=128Mi"
        + " --set nonProductionOptions.createMinio=false "
        + " --reuse-values")
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void deleteStackGresCluster(Container k8s, String namespace, String name) throws Exception {
    LOGGER.info("Delete stackgres-cluster helm chart for cluster with name " + name);
    k8s.execute("sh", "-l", "-c", "helm delete " + name + " --namespace " + namespace)
      .filter(EXCLUDE_TTY_WARNING)
      .forEach(line -> LOGGER.info(line));
  }

  /**
   * It helper method.
   */
  public static void waitUntilOperatorIsReady(CompletableFuture<Void> operator,
      WebTarget operatorClient, Container k8s) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      waitUntilKubernetesOperatorIsReady(k8s);
      return;
    }
    waitUntilLocalOperatorIsReady(operator, operatorClient);
  }

  /**
   * It helper method.
   */
  public static void waitUntilLocalOperatorIsReady(CompletableFuture<Void> operator,
      WebTarget operatorClient) throws Exception {
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
   * It helper method.
   */
  public static void waitUntilKubernetesOperatorIsReady(Container k8s) throws Exception {
    waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
        "kubectl get pod -n stackgres -l app=stackgres-operator -o name"
            + " | xargs -r kubectl describe -n  stackgres ")),
        s -> s.anyMatch(line -> line.matches("^  Ready\\s+True\\s*")), 120, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking availability of"
                + " stackgres-operator pod:\n"
                + s.collect(Collectors.joining("\n"))));
  }

  /**
   * IT helper method.
   * Code has been copied and adapted from {@code QuarkusTestExtension} to allow start/stop
   * quarkus application inside a test.
   */
  public static OperatorRunner createOperator(Container k8s, int port,
      int sslPort, Executor executor) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      return new KubernetesOperatorRunner(k8s, executor);
    }

    return new LocalOperatorRunner(k8s, ItHelper.class, port, sslPort);
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
