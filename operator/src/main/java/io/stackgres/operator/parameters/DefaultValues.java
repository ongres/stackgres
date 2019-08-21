/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.parameters;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class DefaultValues {

  private static final Map<String, String> defaults;

  static {
    defaults = ImmutableMap.<String, String>builder()
        .put("checkpoint_completion_target", "0.9")
        .put("checkpoint_timeout", "15min")
        .put("default_statistics_target", "250")
        .put("wal_level", "logical")
        .put("wal_compression", "on")
        .put("wal_log_hints", "on")
        .put("lc_messages", "C")
        .put("random_page_cost", "2.0")
        .put("track_activity_query_size", "2048")
        .put("archive_mode", "on")
        .put("archive_command", "/bin/true")
        .put("huge_pages", "off")
        .put("shared_preload_libraries", "pg_stat_statements")
        .put("track_io_timing", "on")
        .put("track_functions", "pl")
        .build();
  }

  private DefaultValues() {}

  public static Map<String, String> getDefaultValues() {
    return defaults;
  }

}
