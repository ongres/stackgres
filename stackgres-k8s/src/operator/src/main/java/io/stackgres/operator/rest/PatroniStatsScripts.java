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
    return get("cpuFound");
  }

  public static String getMemoryFound() {
    return get("memoryFound");
  }

  public static String getMemoryUsed() {
    return get("memoryUsed");
  }

  public static String getDiskFound() {
    return get("diskFound");
  }

  public static String getDiskUsed() {
    return get("diskUsed");
  }

  public static String getLoad1m() {
    return get("load1m");
  }

  public static String getLoad5m() {
    return get("load5m");
  }

  public static String getLoad10m() {
    return get("load10m");
  }

  private static String get(String scriptKey) {
    String script = STATS_SCRIPTS.get(scriptKey);
    if (script == null) {
      throw new IllegalStateException(
          "Key " + scriptKey + " not found in patroni-stats.properties");
    }
    return script;
  }

}
