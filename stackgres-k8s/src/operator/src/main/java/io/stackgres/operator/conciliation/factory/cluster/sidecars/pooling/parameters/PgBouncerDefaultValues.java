/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;
import io.stackgres.common.StackGresUtil;
import org.jetbrains.annotations.NotNull;

public class PgBouncerDefaultValues {

  private static final String FILE_PATH = "/pgbouncer-default-values.properties";

  private static final @NotNull Properties DEFAULTS = StackGresUtil.loadProperties(FILE_PATH);

  private PgBouncerDefaultValues() {}

  public static @NotNull Properties getProperties() {
    return DEFAULTS;
  }

  public static @NotNull Map<String, String> getDefaultValues() {
    return Maps.fromProperties(getProperties());
  }

}
