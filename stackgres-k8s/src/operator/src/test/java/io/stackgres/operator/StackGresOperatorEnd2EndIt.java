/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.ongres.junit.docker.Container;
import com.ongres.junit.docker.ContainerParam;
import com.ongres.junit.docker.DockerContainer;
import com.ongres.junit.docker.DockerExtension;
import com.ongres.junit.docker.WhenReuse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DockerExtension({
  @DockerContainer(
      alias = "k8s",
      extendedBy = KindConfiguration.class,
      whenReuse = WhenReuse.ALWAYS,
      stopIfChanged = true)
})
@DisabledIfEnvironmentVariable(named = "DISABLE_E2E", matches = "true")
public class StackGresOperatorEnd2EndIt extends AbstractStackGresOperatorIt {

  private static final Optional<String> E2E_TEST = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_TEST"))
      .orElse(System.getProperty("e2e.test")));

  public static final Optional<Boolean> E2E_DEBUG = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_DEBUG"))
      .orElse(System.getProperty("e2e.debug")))
      .map(Boolean::valueOf);

  @Test
  public void end2EndTest(@ContainerParam("k8s") Container k8s) throws Exception {
    k8s.copyIn(new ByteArrayInputStream(
        ("echo 'Running "
            + (E2E_TEST.map(s -> s + " e2e test").orElse("all e2e tests")) + " from it'\n"
            + "cd /resources/e2e\n"
            + "rm -Rf /resources/e2e/target\n"
            + ItHelper.E2E_ENVVARS + "\n"
            + "export DOCKER_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\"|cut -d '/' -f 2)\"\n"
            + "export " + ItHelper.E2E_ENV_VAR_NAME + "="
                + "\"" + ItHelper.E2E_ENV + "$(echo \"$DOCKER_NAME\" | sed 's/^k8s//')\"\n"
            + "export IMAGE_TAG=" + ItHelper.IMAGE_TAG + "\n"
            + "export K8S_REUSE=true\n"
            + "export K8S_FROM_DIND=true\n"
            + "export E2E_BUILD_OPERATOR=false\n"
            + "export REUSE_OPERATOR=true\n"
            + "export E2E_WAIT_OPERATOR=false\n"
            + "export USE_EXTERNAL_OPERATOR=true\n"
            + "export CLUSTER_CHART_PATH=/resources/stackgres-cluster\n"
            + "export OPERATOR_CHART_PATH=/resources/stackgres-operator\n"
            + (E2E_TEST.map(e2eTests -> "if ! sh " + (E2E_DEBUG.orElse(false) ? "-x" : "")
            + " run-test.sh " + e2eTests + "\n"
            + "then\n"
            + "  sh e2e show_failed_logs\n"
            + "  exit 1\n"
            + "fi\n").orElseGet(() -> "if ! sh " + (E2E_DEBUG.orElse(false) ? "-x" : "")
            + " run-all-tests.sh\n"
            + "then\n"
            + "  sh e2e show_failed_logs\n"
            + "  exit 1\n"
            + "fi\n"))).getBytes(StandardCharsets.UTF_8)), "/run-e2e-from-it.sh");
    k8s.execute("sh", "-e", "/run-e2e-from-it.sh")
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

}
