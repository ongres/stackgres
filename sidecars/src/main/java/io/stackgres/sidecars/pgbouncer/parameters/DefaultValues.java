/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgbouncer.parameters;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class DefaultValues {

  private static final Map<String, String> defaults;

  static {
    defaults = ImmutableMap.<String, String>builder()
        .put("listen_port", "6432")
        .put("listen_addr", "0.0.0.0") //NOPMD
        .put("unix_socket_dir", "/var/run/postgresql")
        .put("auth_type", "md5")
        .put("auth_user", "postgres")
        .put("auth_query", "SELECT usename, passwd FROM pg_shadow WHERE usename=$1")
        .put("admin_users", "postgres")
        .put("stats_users", "postgres")
        .put("user", "postgres")
        .put("pool_mode", "session")
        .put("max_client_conn", "1000")
        .put("max_db_connections", "100")
        .put("max_user_connections", "100")
        .put("default_pool_size", "100")
        .put("ignore_startup_parameters", "extra_float_digits")
        .put("application_name_add_host", "1")
        .build();
  }

  private DefaultValues() {}

  public static Map<String, String> getDefaultValues() {
    return defaults;
  }

}
