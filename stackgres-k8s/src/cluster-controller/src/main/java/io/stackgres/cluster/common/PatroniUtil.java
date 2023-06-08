/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import java.util.regex.Pattern;

import com.ongres.process.FluentProcess;

public interface PatroniUtil {

  Pattern PATRONI_COMMAND_PATTERN =
      Pattern.compile("^[^ ]+ /usr/bin/patroni .*$");

  static void reloadPatroniConfig() {
    final String patroniPid = findPatroniPid();
    FluentProcess.start("sh", "-c",
        String.format("kill -s HUP %s", patroniPid)).join();
  }

  private static String findPatroniPid() {
    return ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> PATRONI_COMMAND_PATTERN.matcher(command).matches())
            .orElse(false))
        .map(ProcessHandle::pid)
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException(
            "Process with pattern " + PATRONI_COMMAND_PATTERN + " not found"));
  }

}
