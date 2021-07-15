/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

public class PgBouncerDefaultValues {

  private static final Map<String, String> DEFAULTS;

  static {
    DEFAULTS = ImmutableMap.<String, String>builder()
        .putAll(readResource().entrySet().stream()
            .filter(e -> !e.getKey().toString().isEmpty())
            .collect(Collectors.toMap(
                e -> e.getKey().toString(), e -> e.getValue().toString())))
        .build();
  }

  private PgBouncerDefaultValues() {}

  private static Properties readResource() {
    Properties properties = new Properties();
    try (InputStream is = PgBouncerDefaultValues.class.getResourceAsStream(
        "/pgbouncer-default-values.properties")) {
      properties.load(is);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return properties;
  }

  public static Map<String, String> getDefaultValues() {
    return DEFAULTS;
  }

}
