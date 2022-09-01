/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public class EnvoyUtil {

  public static final String POSTGRES_PORT_NAME = "pgport";
  public static final String POSTGRES_REPLICATION_PORT_NAME = "pgreplication";
  public static final String PATRONI_RESTAPI_PORT_NAME = "patroniport";
  public static final String BABELFISH_PORT_NAME = "babelfish";
  public static final String ENVOY_PORT_NAME = "envoy";

  public static final int PG_ENTRY_PORT = 7432;
  public static final int PG_REPL_ENTRY_PORT = 7433;
  public static final int PG_POOL_PORT = 6432;
  public static final int PG_PORT = 5432;
  public static final int PATRONI_ENTRY_PORT = 8008;
  public static final int PATRONI_PORT = 8009;
  public static final int BF_ENTRY_PORT = 7434;
  public static final int BF_PORT = 1433;
  public static final int ENVOY_PORT = 8001;

}
