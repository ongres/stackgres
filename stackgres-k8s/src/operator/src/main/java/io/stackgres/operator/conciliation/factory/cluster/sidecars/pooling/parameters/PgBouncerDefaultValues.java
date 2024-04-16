/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.google.common.collect.Maps;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;

public interface PgBouncerDefaultValues {

  enum PgBouncerDefaulValuesProperties {
    PGBOUNCER_DEFAULT_VALUES("/pgbouncer-default-values.properties");

    private final @Nonnull Properties propFile;

    PgBouncerDefaulValuesProperties(@Nonnull String file) {
      this.propFile = StackGresUtil.loadProperties(file);
    }
  }

  static @Nonnull Properties getProperties() {
    return getProperties(StackGresVersion.LATEST);
  }

  static @Nonnull Properties getProperties(
      @Nonnull StackGresVersion version) {
    Objects.requireNonNull(version, "operatorVersion parameter is null");
    return PgBouncerDefaulValuesProperties.PGBOUNCER_DEFAULT_VALUES.propFile;
  }

  static @Nonnull Map<String, String> getDefaultValues() {
    return getDefaultValues(StackGresVersion.LATEST);
  }

  static @Nonnull Map<String, String> getDefaultValues(
      @Nonnull StackGresVersion version) {
    return Maps.fromProperties(getProperties(version));
  }

}
