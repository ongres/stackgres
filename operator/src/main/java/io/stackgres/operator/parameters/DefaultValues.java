/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.parameters;

import java.util.HashMap;
import java.util.Map;

public class DefaultValues {

  private static final Map<String, String> values;

  static {
    values = new HashMap<>();
    values.put("checkpoint_completion_target", "0.9");
    values.put("checkpoint_timeout", "15min");
    values.put("default_statistics_target", "250");
    values.put("wal_level", "logical");
    values.put("wal_compression", "on");
    values.put("wal_log_hints", "on");
    values.put("lc_messages", "C");
    values.put("hot_standby_feedback", "on");
    values.put("min_wal_size", "512MB");
    values.put("max_wal_size", "2GB");
    values.put("random_page_cost", "3.0");
    values.put("track_activity_query_size", "2048");
    values.put("archive_mode", "on");
    values.put("archive_command", "/bin/true");
  }

  private DefaultValues() {}

  public static Map<String, String> getDefaultValues() {
    return values;
  }

}
