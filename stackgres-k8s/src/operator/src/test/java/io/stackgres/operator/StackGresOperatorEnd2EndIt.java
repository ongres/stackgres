/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
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

  private static final Optional<String> RUN_E2E_TEST = Optional.ofNullable(
      Optional.ofNullable(System.getenv("RUN_E2E_TEST"))
      .orElse(System.getProperty("e2e.runTest")));

  @Test
  public void end2EndTest(@ContainerParam("kind") Container kind) throws Exception {
    runAsync(() -> {
      while (true) {
        try (Socket socket = new Socket()) {
          socket.connect(new InetSocketAddress("127.0.0.1", kind.getPort(8001)));
          OutputStream outputStream = socket.getOutputStream();
          while (socket.isConnected()) {
            try {
              outputStream.write('.');
              outputStream.flush();
            } catch (IOException ex) {
              break;
            }
            Thread.sleep(100);
          }
          break;
        } catch (ConnectException ex) {
          continue;
        }
      }
    });
    kind.execute("sh", "-ec",
        "echo 'Running e2e tests from it'\n"
            + "cd /resources/e2e\n"
            + "("
            + "  echo | nc -l -p 8001 -s 0.0.0.0 > /dev/null\n"
            + "  echo Connection lost! Exiting..."
            + "  kill -1 $$\n"
            + ") &"
            + "lock_monitor_pid=$!\n"
            + "trap_callback() {\n"
            + "  kill $lock_monitor_pid\n"
            + "}\n"
            + "trap trap_callback EXIT\n"
            + "export KIND_NAME=\"$(docker inspect -f '{{.Name}}' \"$(hostname)\"|cut -d '/' -f 2)\"\n"
            + "export IMAGE_TAG=" + ItHelper.IMAGE_TAG + "\n"
            + "export REUSE_KIND=true\n"
            + "export USE_KIND_INTERNAL=true\n"
            + "export BUILD_OPERATOR=false\n"
            + "export REUSE_OPERATOR=true\n"
            + "export WAIT_OPERATOR=false\n"
            + "export RESET_NAMESPACES=true\n"
            + "export CLUSTER_CHART_PATH=/resources/stackgres-cluster\n"
            + (RUN_E2E_TEST.isPresent()
            ? "if ! sh run-test.sh " + RUN_E2E_TEST.get() + "\n"
            + "then\n"
            + "  sh e2e show_logs\n"
            + "  exit 1\n"
            + "fi\n"
            : "if ! sh run-all-tests.sh\n"
            + "then\n"
            + "  sh e2e show_logs\n"
            + "  exit 1\n"
            + "fi\n"))
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .forEach(line -> LOGGER.info(line));
  }

}
