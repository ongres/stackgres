/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pooling.parameters;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

public class DefaultValues {

  private static final Map<String, String> DEFAULTS;

  static {
    DEFAULTS = ImmutableMap.<String, String>builder()
        .putAll(readResource().entrySet().stream()
            .filter(e -> !e.getKey().toString().isEmpty())
            .collect(Collectors.toMap(
                e -> e.getKey().toString(), e -> e.getValue().toString())))
        .build();
  }

  private DefaultValues() {}

  private static Properties readResource() {
    Properties properties = new Properties();
    try {
      properties.load(DefaultValues.class.getResourceAsStream(
          "/pgbouncer-default-values.properties"));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    return properties;
  }

  public static Map<String, String> getDefaultValues() {
    return DEFAULTS;
  }

}
