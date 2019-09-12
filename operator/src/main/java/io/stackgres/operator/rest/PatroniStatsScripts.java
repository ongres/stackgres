/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

public class PatroniStatsScripts {

  private static final Map<String, String> STATS_SCRIPTS;

  static {
    STATS_SCRIPTS = ImmutableMap.<String, String>builder()
        .putAll(readResource().entrySet().stream()
            .filter(e -> !e.getKey().toString().isEmpty())
            .collect(Collectors.toMap(
                e -> e.getKey().toString(), e -> e.getValue().toString())))
        .build();
  }

  private PatroniStatsScripts() {}

  private static Properties readResource() {
    Properties properties = new Properties();
    try {
      properties.load(PatroniStatsScripts.class.getResourceAsStream(
          "/patroni-stats.properties"));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    return properties;
  }

  public static String getCpuFound() {
    return get("cpu_found");
  }

  public static String getMemoryFound() {
    return get("memory_found");
  }

  public static String getMemoryUsed() {
    return get("memory_used");
  }

  public static String getDiskFound() {
    return get("disk_found");
  }

  public static String getDiskUsed() {
    return get("disk_used");
  }

  public static String getLoad1m() {
    return get("load_1m");
  }

  public static String getLoad5m() {
    return get("load_5m");
  }

  public static String getLoad10m() {
    return get("load_10m");
  }

  private static String get(String scriptKey) {
    String script = STATS_SCRIPTS.get(scriptKey);
    if (script == null) {
      throw new IllegalStateException("Key " + scriptKey + " not found in patroni-stats.properties");
    }
    return script;
  }

}
