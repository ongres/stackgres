/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import java.util.regex.Pattern;

import com.ongres.process.FluentProcess;

public interface PgBouncerCommandUtil {

  Pattern PGBOUNCER_COMMAND_PATTERN =
      Pattern.compile("^/usr/local/bin/pgbouncer .*$");

  static void reloadPgBouncerConfig() {
    final String pgBouncerPid = findPgBouncerPid();
    FluentProcess.start("sh", "-c",
        String.format("kill -s HUP %s", pgBouncerPid)).join();
  }

  private static String findPgBouncerPid() {
    return ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> PGBOUNCER_COMMAND_PATTERN.matcher(command).matches())
            .orElse(false))
        .map(ProcessHandle::pid)
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException(
            "Process with pattern " + PGBOUNCER_COMMAND_PATTERN + " not found"));
  }

}
