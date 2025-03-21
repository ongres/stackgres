/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.postgres;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.google.common.collect.Maps;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import org.jetbrains.annotations.NotNull;

public interface PostgresDefaultValues {

  enum PostgresDefaulValuesProperties {
    PG_14_VALUES_V_1_17("/v1.17/postgresql-default-values-pg14.properties"),
    PG_13_VALUES_V_1_17("/v1.17/postgresql-default-values-pg13.properties"),
    PG_12_VALUES_V_1_17("/v1.17/postgresql-default-values-pg12.properties"),
    PG_14_VALUES_V_1_16("/v1.16/postgresql-default-values-pg14.properties"),
    PG_13_VALUES_V_1_16("/v1.16/postgresql-default-values-pg13.properties"),
    PG_12_VALUES_V_1_16("/v1.16/postgresql-default-values-pg12.properties"),
    PG_14_VALUES_V_1_15("/v1.15/postgresql-default-values-pg14.properties"),
    PG_13_VALUES_V_1_15("/v1.15/postgresql-default-values-pg13.properties"),
    PG_12_VALUES_V_1_15("/v1.15/postgresql-default-values-pg12.properties");

    private final @NotNull Properties properties;

    PostgresDefaulValuesProperties(@NotNull String file) {
      this.properties = StackGresUtil.loadProperties(file);
    }
  }

  static @NotNull Properties getProperties(
      @NotNull String pgVersion) {
    return getProperties(StackGresVersion.LATEST, pgVersion);
  }

  static @NotNull Properties getProperties(
      @NotNull StackGresVersion version,
      @NotNull String pgVersion) {
    Objects.requireNonNull(version, "operatorVersion parameter is null");
    Objects.requireNonNull(pgVersion, "pgVersion parameter is null");
    int majorVersion = Integer.parseInt(pgVersion.split("\\.")[0]);

    if (version.getVersionAsNumber() <= StackGresVersion.V_1_15.getVersionAsNumber()) {
      if (majorVersion <= 12) {
        return PostgresDefaulValuesProperties.PG_12_VALUES_V_1_15.properties;
      }
      if (majorVersion <= 13) {
        return PostgresDefaulValuesProperties.PG_13_VALUES_V_1_15.properties;
      }
      return PostgresDefaulValuesProperties.PG_14_VALUES_V_1_15.properties;
    }

    if (version.getVersionAsNumber() <= StackGresVersion.V_1_16.getVersionAsNumber()) {
      if (majorVersion <= 12) {
        return PostgresDefaulValuesProperties.PG_12_VALUES_V_1_16.properties;
      }
      if (majorVersion <= 13) {
        return PostgresDefaulValuesProperties.PG_13_VALUES_V_1_16.properties;
      }
      return PostgresDefaulValuesProperties.PG_14_VALUES_V_1_16.properties;
    }

    if (majorVersion <= 12) {
      return PostgresDefaulValuesProperties.PG_12_VALUES_V_1_17.properties;
    }
    if (majorVersion <= 13) {
      return PostgresDefaulValuesProperties.PG_13_VALUES_V_1_17.properties;
    }
    return PostgresDefaulValuesProperties.PG_14_VALUES_V_1_17.properties;
  }

  static @NotNull Map<String, String> getDefaultValues(
      @NotNull String pgVersion) {
    return getDefaultValues(StackGresVersion.LATEST, pgVersion);
  }

  static @NotNull Map<String, String> getDefaultValues(
      @NotNull StackGresVersion version,
      @NotNull String pgVersion) {
    return Maps.fromProperties(getProperties(version, pgVersion));
  }

}
