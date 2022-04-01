/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;

import com.ongres.junit.docker.Container;
import com.ongres.junit.docker.ContainerParam;
import com.ongres.junit.docker.DockerContainer;
import com.ongres.junit.docker.DockerExtension;
import com.ongres.junit.docker.WhenReuse;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@DockerExtension({
    @DockerContainer(
        alias = "k8s",
        extendedBy = K8sConfiguration.class,
        whenReuse = WhenReuse.ALWAYS,
        stopIfChanged = true)
})
@EnabledIfEnvironmentVariable(named = "ENABLE_E2E", matches = "true")
public class StackGresOperatorEnd2EndIt extends AbstractStackGresOperatorIt {

  private static final Optional<String> E2E_TEST = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_TEST"))
          .orElse(System.getProperty("e2e.test")));

  private static final Optional<Boolean> E2E_HANG = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_HANG"))
          .orElse(System.getProperty("e2e.hang")))
      .map(Boolean::valueOf);

  private static final Optional<Boolean> E2E_REPEAT_ON_ERROR = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_REPEAT_ON_ERROR"))
          .orElse(System.getProperty("e2e.repeatOnError")))
      .map(Boolean::valueOf);

  private static final String E2E_SHELL = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_SHELL"))
          .orElse(System.getProperty("e2e.shell")))
      .orElse("sh");

  @Test
  public void end2EndTest(@ContainerParam("k8s") Container k8s) throws Exception {
    try {
      while (true) {
        try {
          k8s.copyIn(new ByteArrayInputStream(
              ("echo 'Running "
                  + (E2E_TEST.map(s -> s + " e2e test").orElse("all e2e tests")) + " from it'\n"
                  + "cd /resources/e2e\n"
                  + "rm -Rf /resources/e2e/target\n"
                  + ItHelper.E2E_ENVVARS + "\n"
                  + "export DOCKER_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\""
                  + "|cut -d '/' -f 2)\"\n"
                  + "export " + ItHelper.E2E_ENV_VAR_NAME + "="
                  + "\"" + ItHelper.E2E_ENV + "$(echo \"$DOCKER_NAME\" | sed 's/^k8s//')\"\n"
                  + "export IMAGE_TAG=" + ItHelper.IMAGE_TAG + "\n"
                  + "export K8S_REUSE=true\n"
                  + "export K8S_FROM_DIND=true\n"
                  + "export E2E_BUILD_IMAGES=false\n"
                  + "export E2E_REUSE_OPERATOR_PODS=true\n"
                  + "export OPERATOR_NAMESPACE=" + namespace + "\n"
                  + "export E2E_WAIT_OPERATOR=false\n"
                  + "export E2E_USE_EXTERNAL_OPERATOR=true\n"
                  + "export E2E_SKIP_SETUP=true\n"
                  + "export CLUSTER_CHART_PATH=/resources/stackgres-cluster\n"
                  + "export OPERATOR_CHART_PATH=/resources/stackgres-operator\n"
                  + "export UI_TESTS_RESOURCES_PATH=/resources/admin-ui\n"
                  + (ItHelper.OPERATOR_IN_KUBERNETES
                      ? ""
                      : "export E2E_OPERATOR_OPTS=\""
                          + ItHelper.getOperatorExtraOptions(k8s, operatorPort) + "\"\n")
                  + "  sh e2e store_operator_values\n"
                  + (E2E_TEST
                      .map(e2eTests -> "if ! " + E2E_SHELL + " " + (ItHelper.E2E_DEBUG ? "-x" : "")
                          + " run-test.sh " + e2eTests + "\n"
                          + "then\n"
                          + "  sh e2e show_failed_logs\n"
                          + "  exit 1\n"
                          + "fi\n")
                      .orElseGet(() -> "if ! " + E2E_SHELL + " " + (ItHelper.E2E_DEBUG ? "-x" : "")
                          + " run-all-tests.sh\n"
                          + "then\n"
                          + "  " + E2E_SHELL + " e2e show_failed_logs\n"
                          + "  exit 1\n"
                          + "fi\n"))).getBytes(StandardCharsets.UTF_8)),
              "/run-e2e-from-it.sh");
          k8s.execute("sh", "-e" + (ItHelper.E2E_DEBUG ? "x" : ""), "/run-e2e-from-it.sh")
              .filter(ItHelper.EXCLUDE_TTY_WARNING)
              .forEach(LOGGER::info);
        } catch (RuntimeException ex) {
          LOGGER.error("exception running tests", ex);
          if (E2E_REPEAT_ON_ERROR.orElse(false)) {
            continue;
          }
          throw ex;
        }
        break;
      }
    } finally {
      try {
        if (Paths.get("target/e2e").toFile().exists()) {
          FileUtils.deleteDirectory(Paths.get("target/e2e").toFile());
        }
        k8s.copyOut("/resources/e2e/target", Paths.get("target/e2e"));
      } catch (Exception ex) {
        LOGGER.error("An error occurred while copying e2e test results", ex);
      }
    }
    if (E2E_HANG.orElse(false)) {
      while (true) {
        Thread.sleep(3600);
      }
    }
  }

}
