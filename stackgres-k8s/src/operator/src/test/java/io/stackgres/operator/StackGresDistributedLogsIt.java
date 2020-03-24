/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

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
      extendedBy = K8sConfiguration.class,
      whenReuse = WhenReuse.ALWAYS,
      stopIfChanged = true)
})
@DisabledIfEnvironmentVariable(named = "DISABLE_IT", matches = "true")
public class StackGresDistributedLogsIt extends AbstractStackGresOperatorIt {

  @Test
  public void createDistributedLogsTest(@ContainerParam("k8s") Container k8s) throws Exception {
    k8s.execute("sh", "-l", "-c",
        "cat << 'EOF' | kubectl create -f -\n"
        + "apiVersion: stackgres.io/v1beta1\n"
        + "kind: SGDistributedLogs\n"
        + "metadata:\n"
        + "  name: test\n"
        + "spec:\n"
        + "  persistentVolume:\n"
        + "    size: 1Gi\n"
        + "EOF\n")
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .forEach(LOGGER::info);
  }

}
