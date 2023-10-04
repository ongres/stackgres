/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import org.jooq.lambda.Seq;

public interface ConfigFilesUtil {

  static boolean configChanged(Path configPath, Path lastConfigPath) throws IOException {
    return configChanged(configPath, lastConfigPath, line -> true);
  }

  static boolean configChanged(Path configPath, Path lastConfigPath, Predicate<String> filter)
      throws IOException {
    return !Files.exists(lastConfigPath)
        || anyLineChanged(configPath, lastConfigPath, filter);
  }

  static boolean anyLineChanged(Path configPath, Path lastConfigPath, Predicate<String> filter)
      throws IOException {
    var lastLines = Files.readAllLines(lastConfigPath).stream().filter(filter).toList();
    var lines = Files.readAllLines(configPath).stream().filter(filter).toList();
    return lastLines.size() != lines.size()
        || !Seq.seq(lastLines)
        .zipWithIndex()
        .allMatch(line -> Seq
            .seq(lines)
            .zipWithIndex()
            .anyMatch(line::equals));
  }

}
