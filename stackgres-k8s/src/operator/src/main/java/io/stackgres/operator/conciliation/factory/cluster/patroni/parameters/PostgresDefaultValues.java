/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;

public class PostgresDefaultValues {

  private enum PostgresVersion {
    PG_DEFAULT_VALUES("/postgresql-default-values.properties"),
    PG13_VALUES("/postgresql-default-values-pg13.properties");

    private final @NotNull ImmutableMap<String, String> propFile;

    PostgresVersion(@NotNull String file) {
      this.propFile = readResource(file);
    }

    private static @NotNull ImmutableMap<String, String> readResource(@NotNull String file) {
      Properties properties = new Properties();
      try (InputStream is = PostgresDefaultValues.class.getResourceAsStream(file)) {
        properties.load(is);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      return Maps.fromProperties(properties);
    }
  }

  public static @NotNull Map<String, String> getDefaultValues(@NotNull String pgVersion) {
    Objects.requireNonNull(pgVersion, "pgVersion parameter is null");
    int majorVersion = Integer.parseInt(pgVersion.split("\\.")[0]);
    if (majorVersion >= 13) {
      return PostgresVersion.PG13_VALUES.propFile;
    }
    return PostgresVersion.PG_DEFAULT_VALUES.propFile;
  }

}
