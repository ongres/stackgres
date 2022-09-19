/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

public interface StackGresPasswordKeys {

  String SUPERUSER_DATABASE = "postgres";
  String SUPERUSER_USERNAME = "postgres";
  String SUPERUSER_USERNAME_ENV = "PATRONI_SUPERUSER_USERNAME";
  String SUPERUSER_PASSWORD_ENV = "PATRONI_SUPERUSER_PASSWORD";
  String SUPERUSER_PASSWORD_KEY = "superuser-password";
  String REPLICATION_USERNAME = "replicator";
  String REPLICATION_USERNAME_ENV = "PATRONI_REPLICATION_USERNAME";
  String REPLICATION_PASSWORD_ENV = "PATRONI_REPLICATION_PASSWORD";
  String REPLICATION_PASSWORD_KEY = "replication-password";
  String AUTHENTICATOR_USERNAME = "authenticator";
  String AUTHENTICATOR_USERNAME_ENV = "PATRONI_authenticator_USERNAME";
  String AUTHENTICATOR_PASSWORD_ENV = "PATRONI_authenticator_PASSWORD";
  String AUTHENTICATOR_PASSWORD_KEY = "authenticator-password";
  String AUTHENTICATOR_OPTIONS_ENV = "PATRONI_authenticator_OPTIONS";
  String PGBOUNCER_ADMIN_USERNAME = "pgbouncer_admin";
  String PGBOUNCER_ADMIN_USERNAME_ENV = "PGBOUNCER_ADMIN_USERNAME";
  String PGBOUNCER_ADMIN_PASSWORD_KEY = "pgbouncer-admin-password";
  String PGBOUNCER_STATS_USERNAME = "pgbouncer_stats";
  String PGBOUNCER_STATS_USERNAME_ENV = "PGBOUNCER_STATS_USERNAME";
  String PGBOUNCER_STATS_PASSWORD_KEY = "pgbouncer-stats-password";
  String BABELFISH_USERNAME = "babelfish";
  String BABELFISH_PASSWORD_KEY = "babelfish-password";
  String BABELFISH_CREATE_USER_SQL_KEY = "babelfish-create-user-sql";
  String RESTAPI_USERNAME = "superuser";
  String RESTAPI_USERNAME_ENV = "PATRONI_RESTAPI_USERNAME";
  String RESTAPI_PASSWORD_ENV = "PATRONI_RESTAPI_PASSWORD";
  String RESTAPI_PASSWORD_KEY = "restapi-password";

}
