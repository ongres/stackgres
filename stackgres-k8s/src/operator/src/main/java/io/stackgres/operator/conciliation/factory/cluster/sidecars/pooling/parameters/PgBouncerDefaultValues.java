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
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;

public class PgBouncerDefaultValues {

  private static final Map<String, String> DEFAULTS;

  static {
    Map<String, String> params = Map.of(
        "listen_port", Integer.toString(EnvoyUtil.PG_POOL_PORT),
        "unix_socket_dir", ClusterStatefulSetPath.PG_RUN_PATH.path());

    DEFAULTS = Stream.concat(params.entrySet().stream(), readResource().entrySet().stream())
        .filter(e -> !e.getKey().toString().isEmpty())
        .sorted((o1, o2) -> o1.getKey().toString().compareTo(o2.getKey().toString()))
        .collect(ImmutableMap.toImmutableMap(
            e -> e.getKey().toString(), e -> e.getValue().toString()));
  }

  private PgBouncerDefaultValues() {}

  private static Properties readResource() {
    Properties properties = new Properties();
    try (InputStream is = PgBouncerDefaultValues.class.getResourceAsStream(
        "/pgbouncer-default-values.properties")) {
      if (is == null) {
        throw new IOException("Couldn't read pgbouncer-default-values.properties");
      }
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
