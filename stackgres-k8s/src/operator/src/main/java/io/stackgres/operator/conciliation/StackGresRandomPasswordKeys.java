/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

public interface StackGresRandomPasswordKeys {

  String SUPERUSER_USER_NAME = "postgres";
  String SUPERUSER_PASSWORD_KEY = "superuser-password";
  String REPLICATION_USER_NAME = "replication";
  String REPLICATION_PASSWORD_KEY = "replication-password";
  String AUTHENTICATOR_USER_NAME = "authenticator";
  String AUTHENTICATOR_PASSWORD_KEY = "authenticator-password";
  String PGBOUNCER_ADMIN_USER_NAME = "pgbouncer_admin";
  String PGBOUNCER_ADMIN_PASSWORD_KEY = "pgbouncer-admin-password";
  String PGBOUNCER_STATS_USER_NAME = "pgbouncer_stats";
  String PGBOUNCER_STATS_PASSWORD_KEY = "pgbouncer-stats-password";
  String RESTAPI_PASSWORD_KEY = "restapi-password";

}
