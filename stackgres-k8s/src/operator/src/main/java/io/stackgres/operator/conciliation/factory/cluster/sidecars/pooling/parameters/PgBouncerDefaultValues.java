/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.google.common.collect.Maps;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import org.jetbrains.annotations.NotNull;

public interface PgBouncerDefaultValues {

  enum PgBouncerDefaulValuesProperties {
    PGBOUNCER_DEFAULT_VALUES("/pgbouncer-default-values.properties");

    private final @NotNull Properties propFile;

    PgBouncerDefaulValuesProperties(@NotNull String file) {
      this.propFile = StackGresUtil.loadProperties(file);
    }
  }

  static @NotNull Properties getProperties() {
    return getProperties(StackGresVersion.LATEST);
  }

  static @NotNull Properties getProperties(
      @NotNull StackGresVersion version) {
    Objects.requireNonNull(version, "operatorVersion parameter is null");
    return PgBouncerDefaulValuesProperties.PGBOUNCER_DEFAULT_VALUES.propFile;
  }

  static @NotNull Map<String, String> getDefaultValues() {
    return getDefaultValues(StackGresVersion.LATEST);
  }

  static @NotNull Map<String, String> getDefaultValues(
      @NotNull StackGresVersion version) {
    return Maps.fromProperties(getProperties(version));
  }

}
