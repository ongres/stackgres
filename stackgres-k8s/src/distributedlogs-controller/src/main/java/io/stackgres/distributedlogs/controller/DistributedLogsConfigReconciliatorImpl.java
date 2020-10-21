/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.ongres.process.FluentProcess;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.StackGresUtil;

@ApplicationScoped
public class DistributedLogsConfigReconciliatorImpl implements DistributedLogsConfigReconciliator {

  private static final String FLUENTD_CONF_FROM_CONFIGMAP_PATH = "/etc/fluentd/fluentd.conf";
  private static final String FLUENTD_CONF_PATH = "/fluentd/fluentd.conf";

  @Override
  public String getFluentdConfigHash() {
    String fluentdConfigHash = StackGresUtil.getMd5Sum(Paths.get(FLUENTD_CONF_FROM_CONFIGMAP_PATH));
    return fluentdConfigHash;
  }

  @Override
  @SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
      justification = "This is not a bug if working with containers")
  public void reloadFluentdConfiguration() throws IOException {
    List<String> oldConfigLines = Files.readAllLines(Paths.get(FLUENTD_CONF_PATH));
    boolean needsRestart = Files.readAllLines(Paths.get(FLUENTD_CONF_FROM_CONFIGMAP_PATH))
        .stream()
        .filter(configMapLine -> configMapLine.matches("^\\s*workers\\s+[0-9]+$"))
        .allMatch(configMapLine -> oldConfigLines
            .stream()
            .filter(line -> line.matches("^\\s*workers\\s+[0-9]+$"))
            .allMatch(line -> line.equals(configMapLine)));
    Files.copy(
        Paths.get(FLUENTD_CONF_FROM_CONFIGMAP_PATH),
        Paths.get(FLUENTD_CONF_PATH),
        StandardCopyOption.REPLACE_EXISTING);
    String fluentdPid = ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> command.startsWith("/usr/bin/ruby /usr/local/bin/fluentd "))
            .orElse(false))
        .map(process -> process.pid())
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Fluentd configmap not found"));
    if (needsRestart) {
      FluentProcess.start("kill", "-s", "SIGINT", fluentdPid).join();
    } else {
      FluentProcess.start("kill", "-s", "SIGUSR2", fluentdPid).join();
    }
  }

}
