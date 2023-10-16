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

  /**
   * Check if source configPath /that must exists) has changed against lastConfigPath (that may
   *  not exists).
   */
  static boolean configChanged(Path configPath, Path lastConfigPath) throws IOException {
    return configChanged(configPath, lastConfigPath, line -> true);
  }

  /**
   * Check if source configPath /that must exists) has changed against lastConfigPath (that may
   *  not exists).
   * The filter is applied to each line of both files before the comparison is performed.
   */
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
