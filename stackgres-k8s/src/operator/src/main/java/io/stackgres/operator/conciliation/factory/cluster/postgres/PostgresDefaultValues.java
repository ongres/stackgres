/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.postgres;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.google.common.collect.Maps;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;

public interface PostgresDefaultValues {

  enum PostgresDefaulValuesProperties {
    PG_14_VALUES("/postgresql-default-values-pg14.properties"),
    PG_13_VALUES("/postgresql-default-values-pg13.properties"),
    PG_12_VALUES("/postgresql-default-values-pg12.properties"),
    PG_14_VALUES_V_1_9("/v1.9/postgresql-default-values-pg14-v1.9.properties"),
    PG_13_VALUES_V_1_9("/v1.9/postgresql-default-values-pg13-v1.9.properties"),
    PG_12_VALUES_V_1_9("/v1.9/postgresql-default-values-pg12-v1.9.properties"),
    PG_14_VALUES_V_1_8("/v1.8/postgresql-default-values-pg14-v1.8.properties"),
    PG_13_VALUES_V_1_8("/v1.8/postgresql-default-values-pg13-v1.8.properties"),
    PG_12_VALUES_V_1_8("/v1.8/postgresql-default-values-pg12-v1.8.properties");

    private final @Nonnull Properties properties;

    PostgresDefaulValuesProperties(@Nonnull String file) {
      this.properties = StackGresUtil.loadProperties(file);
    }
  }

  static @Nonnull Properties getProperties(
      @Nonnull String pgVersion) {
    return getProperties(StackGresVersion.LATEST, pgVersion);
  }

  static @Nonnull Properties getProperties(
      @Nonnull StackGresVersion version,
      @Nonnull String pgVersion) {
    Objects.requireNonNull(version, "operatorVersion parameter is null");
    Objects.requireNonNull(pgVersion, "pgVersion parameter is null");
    int majorVersion = Integer.parseInt(pgVersion.split("\\.")[0]);

    if (version.getVersionAsNumber() <= StackGresVersion.V_1_8.getVersionAsNumber()) {
      if (majorVersion <= 12) {
        return PostgresDefaulValuesProperties.PG_12_VALUES_V_1_8.properties;
      }
      if (majorVersion <= 13) {
        return PostgresDefaulValuesProperties.PG_13_VALUES_V_1_8.properties;
      }
      return PostgresDefaulValuesProperties.PG_14_VALUES_V_1_8.properties;
    }

    if (version.getVersionAsNumber() <= StackGresVersion.V_1_9.getVersionAsNumber()) {
      if (majorVersion <= 12) {
        return PostgresDefaulValuesProperties.PG_12_VALUES_V_1_9.properties;
      }
      if (majorVersion <= 13) {
        return PostgresDefaulValuesProperties.PG_13_VALUES_V_1_9.properties;
      }
      return PostgresDefaulValuesProperties.PG_14_VALUES_V_1_9.properties;
    }

    if (majorVersion <= 12) {
      return PostgresDefaulValuesProperties.PG_12_VALUES.properties;
    }
    if (majorVersion <= 13) {
      return PostgresDefaulValuesProperties.PG_13_VALUES.properties;
    }
    return PostgresDefaulValuesProperties.PG_14_VALUES.properties;
  }

  static @Nonnull Map<String, String> getDefaultValues(
      @Nonnull String pgVersion) {
    return getDefaultValues(StackGresVersion.LATEST, pgVersion);
  }

  static @Nonnull Map<String, String> getDefaultValues(
      @Nonnull StackGresVersion version,
      @Nonnull String pgVersion) {
    return Maps.fromProperties(getProperties(version, pgVersion));
  }

}
