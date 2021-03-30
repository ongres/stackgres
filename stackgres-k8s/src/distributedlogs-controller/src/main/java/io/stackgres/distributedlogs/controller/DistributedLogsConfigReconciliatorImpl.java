/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.io.IOException;
import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;

import com.ongres.process.FluentProcess;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.StackGresUtil;

@ApplicationScoped
public class DistributedLogsConfigReconciliatorImpl implements DistributedLogsConfigReconciliator {

  private static final String FLUENTD_CONF_FROM_CONFIGMAP_PATH = "/etc/fluentd/fluentd.conf";

  @Override
  public String getFluentdConfigHash() {
    return StackGresUtil.getMd5Sum(Paths.get(FLUENTD_CONF_FROM_CONFIGMAP_PATH));
  }

  @Override
  @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
      justification = "This is not a bug if working with containers")
  public void reloadFluentdConfiguration() throws IOException {

    String fluentdPid = ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> command.startsWith("/usr/bin/ruby /usr/local/bin/fluentd "))
            .orElse(false))
        .map(ProcessHandle::pid)
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Fluentd configmap not found"));
    FluentProcess.start("sh", "-c", "kill -s INT " + fluentdPid).join();
  }

}
