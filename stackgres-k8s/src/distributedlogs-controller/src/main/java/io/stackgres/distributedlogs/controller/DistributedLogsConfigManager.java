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
import java.nio.file.StandardOpenOption;

import com.ongres.process.FluentProcess;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.StackGresUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
@SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "This is not a bug if working with containers")
public class DistributedLogsConfigManager {

  private static final Path FLUENTD_CONF_FROM_CONFIGMAP_PATH =
      Paths.get("/etc/fluentd/fluentd.conf");
  private static final Path FLUENTD_CONF_PATH = Paths.get("/fluentd/fluentd.conf");
  private static final Path FLUENTD_CONF_MD5_PATH = Paths.get("/fluentd/fluentd.conf.md5");

  public String getFluentdConfigHash() {
    return StackGresUtil.getMd5Sum(FLUENTD_CONF_FROM_CONFIGMAP_PATH);
  }

  public void reloadFluentdConfiguration() throws IOException {
    if (Files.exists(FLUENTD_CONF_MD5_PATH)
        && Files.readString(FLUENTD_CONF_MD5_PATH).equals(getFluentdConfigHash())) {
      return;
    }
    boolean needsRestart = Files.exists(FLUENTD_CONF_PATH)
        && !Seq.seq(Files.readAllLines(FLUENTD_CONF_FROM_CONFIGMAP_PATH))
        .filter(configMapLine -> configMapLine.matches("^\\s*workers\\s+[0-9]+$"))
        .zipWithIndex()
        .allMatch(Unchecked.predicate(
            configMapLine -> Seq.seq(Files.readAllLines(FLUENTD_CONF_PATH))
            .filter(line -> line.matches("^\\s*workers\\s+[0-9]+$"))
            .zipWithIndex()
            .anyMatch(line -> line.equals(configMapLine))));
    Files.copy(
        FLUENTD_CONF_FROM_CONFIGMAP_PATH,
        FLUENTD_CONF_PATH,
        StandardCopyOption.REPLACE_EXISTING);
    String fluentdPid = ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> command.startsWith("/usr/local/bin/ruby /usr/local/bin/fluentd "))
            .orElse(false))
        .map(ProcessHandle::pid)
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Fluentd process not found"));
    if (needsRestart) {
      FluentProcess.start("sh", "-c", "kill -s INT " + fluentdPid).join();
    } else {
      FluentProcess.start("sh", "-c", "kill -s USR2 " + fluentdPid).join();
    }
    Files.writeString(FLUENTD_CONF_MD5_PATH, getFluentdConfigHash(),
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

}
