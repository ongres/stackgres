/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.util.Optional;

import com.ongres.junit.docker.Container;
import com.ongres.junit.docker.ContainerParam;
import com.ongres.junit.docker.DockerContainer;
import com.ongres.junit.docker.DockerExtension;
import com.ongres.junit.docker.WhenReuse;

import org.junit.jupiter.api.Test;

@DockerExtension({
  @DockerContainer(
      alias = "kind",
      extendedBy = KindConfiguration.class,
      whenReuse = WhenReuse.ALWAYS,
      stopIfChanged = true)
})
public class StackGresOperatorEnd2EndIt extends AbstractStackGresOperatorIt {

  private static final Optional<String> E2E_TEST = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_TEST"))
      .orElse(System.getProperty("e2e.test")));

  private static final Optional<Boolean> E2E_DEBUG = Optional.ofNullable(
      Optional.ofNullable(System.getenv("E2E_DEBUG"))
      .orElse(System.getProperty("e2e.debug")))
      .map(Boolean::valueOf);

  @Test
  public void end2EndTest(@ContainerParam("kind") Container kind) throws Exception {
    kind.execute("sh", "-ec",
        "echo 'Running "
            + (E2E_TEST.isPresent()
                ? E2E_TEST.get() + " e2e test"
                    : "all e2e tests") + " from it'\n"
            + "cd /resources/e2e\n"
            + "export KIND_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\"|cut -d '/' -f 2)\"\n"
            + "export IMAGE_TAG=" + ItHelper.IMAGE_TAG + "\n"
            + "export REUSE_K8S=true\n"
            + "export USE_KIND_INTERNAL=true\n"
            + "export BUILD_OPERATOR=false\n"
            + "export REUSE_OPERATOR=true\n"
            + "export WAIT_OPERATOR=false\n"
            + "export RESET_NAMESPACES=true\n"
            + "export USE_EXTERNAL_OPERATOR=true\n"
            + "export CLUSTER_CHART_PATH=/resources/stackgres-cluster\n"
            + (E2E_TEST.isPresent()
            ? "if ! sh " + (E2E_DEBUG.orElse(false) ? "-x" : "")
                + " run-test.sh " + E2E_TEST.get() + "\n"
            + "then\n"
            + "  sh e2e show_failed_logs\n"
            + "  exit 1\n"
            + "fi\n"
            : "if ! sh " + (E2E_DEBUG.orElse(false) ? "-x" : "")
                + " run-all-tests.sh\n"
            + "then\n"
            + "  sh e2e show_failed_logs\n"
            + "  exit 1\n"
            + "fi\n"))
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

}
