/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

import com.github.dockerjava.api.exception.DockerException;
import com.google.common.collect.ImmutableList;
import com.ongres.junit.docker.Container;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger("ItHelper");

  public static final boolean OPERATOR_IN_KUBERNETES =
      Boolean.parseBoolean(System.getenv("OPERATOR_IN_KUBERNETES"));
  public static final String IMAGE_TAG = Optional.ofNullable(System.getenv("IMAGE_TAG"))
      .orElse("main-jvm");
  public static final Predicate<String> EXCLUDE_TTY_WARNING =
      line -> !line.equals("stdin: is not a tty");

  public static final String E2E_ENV = Optional.ofNullable(System.getenv("E2E_ENV"))
      .orElse("kind");

  public static final String E2E_ENV_VAR_NAME = E2E_ENV.toUpperCase(Locale.US) + "_NAME";

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
        .forEach(LOGGER::info);
  }

  /**
   * IT helper method.
   */
  public static void copyResources(Container k8s) throws Exception {
    k8s.execute("rm", "-Rf", "/resources").forEach(LOGGER::info);
    Path k8sPath = Paths.get("../..");
    k8s.copyIn(k8sPath.resolve("install/helm/stackgres-operator"),
        "/resources/stackgres-operator");
    k8s.copyIn(k8sPath.resolve("install/helm/stackgres-cluster"),
        "/resources/stackgres-cluster");
    k8s.copyIn(k8sPath.resolve("e2e"), "/resources/e2e");
    k8s.copyIn(k8sPath.resolve("src/admin-ui/cypress"), "/resources/admin-ui/cypress");
    k8s.copyIn(k8sPath.resolve("src/admin-ui/cypress.json"), "/resources/admin-ui/cypress.json");
  }

  public static String generateOperatorNamespace(Container k8s) throws Exception {
    return k8s.execute("sh", "/resources/e2e/e2e", "generate_operator_namespace")
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .peek(line -> LOGGER.info("Generated operator namespace: {}", line))
        .collect(Collectors.joining());
  }

  /**
   * It helper method.
   */
  public static void resetKind(Container k8s, String namespace) throws Exception {
    if (Optional.ofNullable(System.getenv("K8S_REUSE"))
        .map(Boolean::parseBoolean)
        .orElse(true)) {
      LOGGER.info("Reusing {}", E2E_ENV);
      k8s.copyIn(new ByteArrayInputStream(
          ("cd /resources/e2e\n"
              + E2E_ENVVARS + "\n"
              + "export DOCKER_NAME=\"$(docker inspect -f '{{.Name}}' "
              + "\"$(hostname)\"|cut -d '/' -f 2)\"\n"
              + "export " + E2E_ENV_VAR_NAME + "="
              + "\"" + E2E_ENV + "$(echo \"$DOCKER_NAME\" | sed 's/^k8s//')\"\n"
              + "export K8S_FROM_DIND=true\n"
              + "export IMAGE_TAG=" + ItHelper.IMAGE_TAG + "\n"
              + "export CLUSTER_CHART_PATH=/resources/stackgres-cluster\n"
              + "export OPERATOR_CHART_PATH=/resources/stackgres-operator\n"
              + "export UI_TESTS_RESOURCES_PATH=/resources/admin-ui\n"
              + "export OPERATOR_NAMESPACE=" + namespace + "\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e reuse_k8s\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e setup_helm\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e k8s_webhook_cleanup\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e helm_cleanup\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e k8s_async_cleanup\n"
              + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e create_operator_certificate\n"
              + "mkdir -p /resources/certs\n"
              + "cp -v /resources/e2e/target/server-key.pem /resources/certs\n"
              + "cp -v /resources/e2e/target/server-pub.pem /resources/certs\n"
              + "cp -v /resources/e2e/target/server.crt /resources/certs\n"
              + (OPERATOR_IN_KUBERNETES
                  ? "sh " + (E2E_DEBUG ? "-x" : "") + " e2e load_operator_k8s\n"
                  : "")
              + (OPERATOR_IN_KUBERNETES ? ""
                  : "sh " + (E2E_DEBUG ? "-x" : "")
                      + " e2e load_certificate_k8s /resources/e2e/target/server.crt\n"))
                          .getBytes(StandardCharsets.UTF_8)),
          "/reuse-k8s.sh");
      k8s.execute("sh", "-e", "/reuse-k8s.sh")
          .filter(ItHelper.EXCLUDE_TTY_WARNING)
          .forEach(LOGGER::info);
      if (Paths.get("target/certs").toFile().exists()) {
        FileUtils.deleteDirectory(Paths.get("target/certs").toFile());
      }
      Files.createDirectory(Paths.get("target/certs"));
      Seq.of("server-key.pem", "server-pub.pem", "server.crt")
          .forEach(Unchecked.consumer(file -> k8s.copyOut(
              "/resources/e2e/target/" + file, Paths.get("target/certs", file))));
      return;
    }
    LOGGER.info("Restarting {}", E2E_ENV);
    k8s.copyIn(new ByteArrayInputStream(
        ("cd /resources/e2e\n"
            + E2E_ENVVARS + "\n"
            + "export DOCKER_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\""
            + "|cut -d '/' -f 2)\"\n"
            + "export " + E2E_ENV_VAR_NAME + "="
            + "\"" + E2E_ENV + "$(echo \"$DOCKER_NAME\" | sed 's/^k8s//')\"\n"
            + "export K8S_FROM_DIND=true\n"
            + "export IMAGE_TAG=" + ItHelper.IMAGE_TAG + "\n"
            + "export CLUSTER_CHART_PATH=/resources/stackgres-cluster\n"
            + "export OPERATOR_CHART_PATH=/resources/stackgres-operator\n"
            + "export UI_TESTS_RESOURCES_PATH=/resources/admin-ui\n"
            + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e reset_k8s\n"
            + "sh " + (E2E_DEBUG ? "-x" : "") + " e2e setup_helm\n"
            + (OPERATOR_IN_KUBERNETES
                ? "sh " + (E2E_DEBUG ? "-x" : "") + " e2e load_operator_k8s\n"
                : "")
            + (OPERATOR_IN_KUBERNETES ? ""
                : "sh " + (E2E_DEBUG ? "-x" : "")
                    + " e2e load_certificate_k8s /resources/certs/server.crt\n"))
                        .getBytes(StandardCharsets.UTF_8)),
        "/restart-k8s.sh");
    k8s.execute("sh", "-e", "/restart-k8s.sh")
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

  /**
   * It helper method.
   */
  public static void createNamespace(Container k8s, String namespace) throws Exception {
    LOGGER.info("Create namespace '{}'", namespace);
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
    if (firstPointIndex.isEmpty()) {
      return host;
    }
    String namespace = host.substring(firstPointIndex.get() + 1,
        secondPointIndex.orElse(host.length()));
    String serviceName = host.substring(0, firstPointIndex.get());
    String exposedServiceName = serviceName + "-exposed";
    LOGGER.info("Create service {} to expose service {} in namespace {}", exposedServiceName, serviceName, namespace);
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
            + " do [ \"$(date +%s)\" -lt \"$END\" ]; sleep 1; done;")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
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
      final String jvmSuffix = "-jvm";
      String adminUiImageTag = IMAGE_TAG.endsWith(jvmSuffix)
          ? IMAGE_TAG.substring(0, IMAGE_TAG.length() - jvmSuffix.length())
          : IMAGE_TAG;
      LOGGER.info("Installing stackgres-operator helm chart");
      k8s.execute("sh", "-l", "-c", "kubectl create namespace " + namespace + " || true")
          .filter(EXCLUDE_TTY_WARNING)
          .forEach(LOGGER::info);
      k8s.execute("sh", "-l", "-c" + (E2E_DEBUG ? "x" : ""), "helm upgrade --install"
          + " stackgres-operator"
          + " --namespace " + namespace
          + " /resources/stackgres-operator"
          + " --set-string operator.image.name=stackgres/operator"
          + " --set-string operator.image.tag=" + IMAGE_TAG
          + " --set-string operator.image.pullPolicy=Never"
          + " --set-string restapi.image.name=stackgres/restapi"
          + " --set-string restapi.image.tag=" + IMAGE_TAG
          + " --set-string restapi.image.pullPolicy=Never"
          + " --set-string adminui.image.name=stackgres/admin-ui"
          + " --set-string adminui.image.tag=" + adminUiImageTag
          + " --set-string adminui.image.pullPolicy=Never")
          .filter(EXCLUDE_TTY_WARNING)
          .forEach(LOGGER::info);
      k8s.execute("sh", "-l", "-c",
          "OPERATOR_NAMESPACE='" + namespace + "' sh " + (E2E_DEBUG ? "-x" : "")
              + " /resources/e2e/e2e store_operator_values\n")
          .filter(EXCLUDE_TTY_WARNING)
          .forEach(LOGGER::info);
      return;
    }

    LOGGER.info("Installing stackgres-operator helm chart without operator container");
    k8s.execute("sh", "-l", "-c", "kubectl create namespace " + namespace + " || true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
    k8s.execute("sh", "-l", "-c", "helm upgrade --install"
        + " stackgres-operator"
        + " --namespace " + namespace
        + " /resources/stackgres-operator"
        + getOperatorExtraOptions(k8s, port))
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
    k8s.execute("sh", "-l", "-c",
        "OPERATOR_NAMESPACE='" + namespace + "' sh " + (E2E_DEBUG ? "-x" : "")
            + " /resources/e2e/e2e store_operator_values\n")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

  public static String getOperatorExtraOptions(Container k8s, int port) throws Exception {
    String dockerInterfaceIp = getDockerInterfaceIp(k8s);
    return " --set deploy.create=false"
        + " --set-string developer.externalOperatorIp=" + dockerInterfaceIp
        + " --set developer.externalOperatorPort=" + port
        + " --set-string cert.crt=" + Base64.getEncoder().encodeToString(
            Files.readAllBytes(Paths.get("target/certs/server.crt")))
        + " --set-string cert.key=" + Base64.getEncoder().encodeToString(
            Files.readAllBytes(Paths.get("target/certs/server-key.pem")))
        + " --set-string cert.jwtRsaKey=" + Base64.getEncoder().encodeToString(
            Files.readAllBytes(Paths.get("target/certs/server-key.pem")))
        + " --set-string cert.jwtRsaPub=" + Base64.getEncoder().encodeToString(
            Files.readAllBytes(Paths.get("target/certs/server-pub.pem")));
  }

  public static String getDockerInterfaceIp(Container k8s)
      throws DockerException, InterruptedException, IOException {
    return k8s.execute("sh", "-l", "-c", ""
        + "CONTAINER_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\"|cut -d '/' -f 2)\"\n"
        + "ENV_NAME=\"" + E2E_ENV + "$(echo \"$CONTAINER_NAME\" | sed 's/^k8s//')\"\n"
        + "NETWORK_NAME=\"$(docker inspect \"$ENV_NAME-control-plane\""
        + " -f '{{ range $key, $value := .NetworkSettings.Networks }}{{ printf \"%s\\n\" $key }}"
        + "{{ end }}'"
        + " | head -n 1)\"\n"
        + "docker network inspect \"$NETWORK_NAME\" -f '{{ (index .IPAM.Config 0).Gateway }}'")
        .filter(EXCLUDE_TTY_WARNING)
        .collect(Collectors.joining());
  }

  public static String getKubernetesMasterIp(Container k8s)
      throws DockerException, InterruptedException, IOException {
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
        + " --set-string configurations.backupconfig.baseBackups.cronSchedule='*/1 * * * *'")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

  /**
   * It helper method.
   */
  public static void installStackGresCluster(Container k8s, String namespace, String name,
      int instances) throws Exception {
    LOGGER.info("Deleting if exists stackgres-cluster helm chart for cluster with name {}", name);
    k8s.execute("sh", "-l", "-c", "helm delete " + name + " --namespace " + namespace + "|| true")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
    LOGGER.info("Installing stackgres-cluster helm chart for cluster with name {}", name);
    k8s.execute("sh", "-l", "-c", "helm install "
        + name
        + " --namespace " + namespace
        + " /resources/stackgres-cluster"
        + " --set configurations.create=false"
        + " --set instanceProfiles=null"
        + " --set-string cluster.name=" + name
        + " --set cluster.instances=" + instances
        + " --set-string cluster.pods.persistentVolume.size=128Mi")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

  /**
   * It helper method.
   */
  public static void upgradeStackGresCluster(Container k8s, String namespace, String name,
      int instances) throws Exception {
    LOGGER.info("Upgrade stackgres-cluster helm chart for cluster with name {}", name);
    k8s.execute("sh", "-l", "-c", "helm upgrade "
        + name
        + " /resources/stackgres-cluster "
        + " --namespace " + namespace
        + " --set configurations.create=false"
        + " --set instanceProfiles=null"
        + " --set-string cluster.name=" + name
        + " --set cluster.instances=" + instances
        + " --set-string cluster.pods.persistentVolume.size=128Mi"
        + " --reuse-values")
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

  /**
   * It helper method.
   */
  public static void deleteStackGresCluster(Container k8s, String namespace, String name)
      throws Exception {
    LOGGER.info("Delete stackgres-cluster helm chart for cluster with name {}", name);
    k8s.execute("sh", "-l", "-c", "helm delete " + name + " --namespace " + namespace)
        .filter(EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

  /**
   * It helper method.
   */
  public static void waitUntilOperatorIsReady(CompletableFuture<Void> operator,
      WebTarget operatorClient, Container k8s, String namespace) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      waitUntilKubernetesOperatorIsReady(k8s, namespace);
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
            .get().getStatusInfo().equals(Response.Status.OK)) {
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
  public static void waitUntilKubernetesOperatorIsReady(Container k8s, String namespace)
      throws Exception {
    waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
        "kubectl get pod -n " + namespace + " -l app=stackgres-operator -o name"
            + " | xargs -r kubectl describe -n " + namespace)),
        s -> s.anyMatch(line -> line.matches("^  Ready\\s+True\\s*")), 120, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking availability of"
                + " stackgres-operator pod:\n"
                + s.collect(Collectors.joining("\n"))));
  }

  /**
   * IT helper method. Code has been copied and adapted from {@code QuarkusTestExtension} to allow
   * start/stop quarkus application inside a test.
   */
  public static OperatorRunner createOperator(Container k8s, String namespace, int port,
      int sslPort, Executor executor) throws Exception {
    if (OPERATOR_IN_KUBERNETES) {
      return new KubernetesOperatorRunner(k8s, namespace, executor);
    }

    // return new LocalOperatorRunner(k8s, namespace, ItHelper.class, port, sslPort);
    throw new Exception();
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

  private ItHelper() {}

}
