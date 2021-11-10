/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.v09;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresUtil;
import org.jetbrains.annotations.NotNull;

public class PgBouncerDefaultValues {

  private static final String FILE_PATH = "/pgbouncer-v095-default-values.properties";

  private static final @NotNull Properties DEFAULTS = StackGresUtil.loadProperties(FILE_PATH);

  private PgBouncerDefaultValues() {
  }

  public static @NotNull Properties getProperties() {
    return DEFAULTS;
  }

  public static @NotNull Map<String, String> getDefaultValues() {
    return ImmutableMap.<String, String>builder()
        .putAll(getProperties().entrySet().stream()
            .filter(e -> !e.getKey().toString().isEmpty())
            .collect(Collectors.toMap(
                e -> e.getKey().toString(), e -> e.getValue().toString())))
        .build();
  }

}
