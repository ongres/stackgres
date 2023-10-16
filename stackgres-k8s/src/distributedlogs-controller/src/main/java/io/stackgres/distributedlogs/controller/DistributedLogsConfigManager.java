/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.enterprise.context.ApplicationScoped;

import com.ongres.process.FluentProcess;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ConfigFilesUtil;
import io.stackgres.common.StackGresUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "This is not a bug if working with containers")
public class DistributedLogsConfigManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      DistributedLogsConfigManager.class);

  private static final Path LAST_FLUENTD_CONF_PATH = Paths.get("/fluentd/last-fluentd.conf");
  public static final Path FLUENTD_CONF_PATH = Paths.get("/etc/fluentd/fluentd.conf");

  public void reloadFluentdConfiguration() throws IOException {
    if (!ConfigFilesUtil.configChanged(
        FLUENTD_CONF_PATH, LAST_FLUENTD_CONF_PATH)) {
      return;
    }
    boolean needsRestart = ConfigFilesUtil.configChanged(
        FLUENTD_CONF_PATH, LAST_FLUENTD_CONF_PATH,
        line -> line.matches("^\\s*workers\\s+[0-9]+$"));
    String fluentdPid = ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> command.startsWith("/usr/local/bin/ruby /usr/local/bin/fluentd "))
            .orElse(false))
        .map(ProcessHandle::pid)
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Fluentd process not found"));
    if (needsRestart) {
      LOGGER.info("Reloading fluentd configuration (with restart)");
      FluentProcess.start("sh", "-c", "kill -s INT " + fluentdPid).join();
    } else {
      LOGGER.info("Reloading fluentd configuration");
      FluentProcess.start("sh", "-c", "kill -s USR2 " + fluentdPid).join();
    }
    Files.copy(
        FLUENTD_CONF_PATH,
        LAST_FLUENTD_CONF_PATH,
        StandardCopyOption.REPLACE_EXISTING);
  }

  public String getFluentdConfigHash() {
    return StackGresUtil.getMd5Sum(FLUENTD_CONF_PATH);
  }

}
