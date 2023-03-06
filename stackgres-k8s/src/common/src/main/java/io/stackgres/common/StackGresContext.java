/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import com.google.common.collect.ImmutableMap;

public interface StackGresContext {

  String STACKGRES_KEY_PREFIX = "stackgres.io/";

  String APP_KEY = "app";
  String CLUSTER_APP_NAME = "StackGresCluster";
  String CLUSTER_NAME_KEY = "cluster-name";
  String CLUSTER_UID_KEY = "cluster-uid";
  String CLUSTER_SCOPE_KEY = "cluster-scope";
  String CLUSTER_NAMESPACE_KEY = "cluster-namespace";
  String RIGHT_VALUE = Boolean.TRUE.toString();
  String WRONG_VALUE = Boolean.FALSE.toString();
  String CLUSTER_KEY = "cluster";
  String BACKUP_KEY = "backup";
  String DB_OPS_KEY = "db-ops";
  String SCHEDULED_BACKUP_KEY = "scheduled-backup";
  String SCHEDULED_BACKUP_JOB_NAME_KEY = "scheduled-backup-job-name";
  String DISRUPTIBLE_KEY = "disruptible";
  String DISTRIBUTED_LOGS_APP_NAME = "StackGresDistributedLogs";
  String DISTRIBUTED_LOGS_CLUSTER_NAME_KEY = "distributed-logs-name";
  String DISTRIBUTED_LOGS_CLUSTER_NAMESPACE_KEY = "distributed-logs-namespace";
  String DISTRIBUTED_LOGS_CLUSTER_UID_KEY = "distributed-logs-uid";
  String DISTRIBUTED_LOGS_CLUSTER_SCOPE_KEY = "distributed-logs-scope";
  String DBOPS_APP_NAME = "StackGresDbOps";
  String DBOPS_NAME_KEY = "dbops-name";
  String DBOPS_UID_KEY = "dbops-uid";
  String DBOPS_NAMESPACE_KEY = "dbops-namespace";
  String BACKUP_APP_NAME = "StackGresBackup";
  String BACKUP_NAME_KEY = "backup-name";
  String BACKUP_UID_KEY = "backup-uid";
  String BACKUP_NAMESPACE_KEY = "backup-namespace";

  String REST_APIUSER_KEY = "apiUsername";
  String REST_K8SUSER_KEY = "k8sUsername";
  String REST_PASSWORD_KEY = "password";

  String VERSION_KEY = STACKGRES_KEY_PREFIX + "operatorVersion";
  String RECONCILIATION_PAUSE_KEY = STACKGRES_KEY_PREFIX + "reconciliation-pause";
  String RECONCILIATION_PAUSE_UNTIL_RESTART_KEY =
      STACKGRES_KEY_PREFIX + "reconciliation-pause-until-restart";
  String CLUSTER_CONTROLLER_VERSION_KEY = STACKGRES_KEY_PREFIX + "cluster-controller-version";
  String DISTRIBUTEDLOGS_CONTROLLER_VERSION_KEY =
      STACKGRES_KEY_PREFIX + "distributedlogs-controller-version";
  String POSTGRES_VERSION_KEY = STACKGRES_KEY_PREFIX + "postgresql-version";
  String PATRONI_VERSION_KEY = STACKGRES_KEY_PREFIX + "patroni-version";
  String ENVOY_VERSION_KEY = STACKGRES_KEY_PREFIX + "envoy-version";
  String PGBOUNCER_VERSION_KEY = STACKGRES_KEY_PREFIX + "pgbouncer-version";
  String PROMETHEUS_POSTGRES_EXPORTER_VERSION_KEY =
      STACKGRES_KEY_PREFIX + "prometheus-postgres-exporter-version";
  String FLUENTBIT_VERSION_KEY = STACKGRES_KEY_PREFIX + "fluentbit-version";
  String FLUENTD_VERSION_KEY = STACKGRES_KEY_PREFIX + "fluentd-version";
  String CONTAINER_KEY = STACKGRES_KEY_PREFIX + "container";
  ImmutableMap<String, String> ANNOTATIONS_TO_COMPONENT =
      ImmutableMap.<String, String>builder()
      .put(CLUSTER_CONTROLLER_VERSION_KEY, "cluster-controller")
      .put(DISTRIBUTEDLOGS_CONTROLLER_VERSION_KEY, "distributedlogs-controller")
      .put(POSTGRES_VERSION_KEY, "postgresql")
      .put(PATRONI_VERSION_KEY, "patroni")
      .put(ENVOY_VERSION_KEY, "envoy")
      .put(PGBOUNCER_VERSION_KEY, "pgbouncer")
      .put(PROMETHEUS_POSTGRES_EXPORTER_VERSION_KEY, "prometheus-postgres-exporter")
      .put(FLUENTBIT_VERSION_KEY, "fluent-bit")
      .put(FLUENTD_VERSION_KEY, "fluentd")
      .build();

  String AUTH_KEY = "api.stackgres.io/auth";
  String AUTH_USER_VALUE = "user";

  String LOCK_SERVICE_ACCOUNT_KEY = "lockServiceAccount";
  String LOCK_POD_KEY = "lockPod";
  String LOCK_TIMESTAMP_KEY = "lockTimestamp";

}
