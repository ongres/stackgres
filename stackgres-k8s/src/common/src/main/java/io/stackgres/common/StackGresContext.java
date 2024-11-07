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
  String RESTAPI_KEY = "restapi";
  String GRAFANA_INTEGRATION_KEY = "grafana-integration";
  String COLLECTOR_KEY = "collector";
  String CLUSTER_KEY = "cluster";
  String BACKUP_KEY = "backup";
  String DBOPS_KEY = "db-ops";
  String SCHEDULED_BACKUP_KEY = "scheduled-backup";
  String SCHEDULED_BACKUP_JOB_NAME_KEY = "scheduled-backup-job-name";
  String DISRUPTABLE_KEY = "disruptible";
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
  String RECONCILIATION_INITIALIZATION_BACKUP_KEY = "reconciliation-init";
  String SHARDED_CLUSTER_APP_NAME = "StackGresShardedCluster";
  String SHARDED_CLUSTER_NAME_KEY = "shardedcluster-name";
  String SHARDED_CLUSTER_UID_KEY = "shardedcluster-uid";
  String SHARDED_CLUSTER_NAMESPACE_KEY = "shardedcluster-namespace";
  String COORDINATOR_KEY = "coordinator";
  String SHARDS_KEY = "shards";
  String SHARDED_BACKUP_KEY = "sharded-backup";
  String SHARDED_DBOPS_KEY = "sharded-db-ops";
  String SCHEDULED_SHARDED_BACKUP_KEY = "scheduled-sharded-backup";
  String SCHEDULED_SHARDED_BACKUP_JOB_NAME_KEY = "scheduled-sharded-backup-job-name";
  String SHARDED_BACKUP_APP_NAME = "StackGresShardedBackup";
  String SHARDED_BACKUP_NAME_KEY = "sharded-backup-name";
  String SHARDED_BACKUP_UID_KEY = "sharded-backup-uid";
  String SHARDED_BACKUP_NAMESPACE_KEY = "sharded-backup-namespace";
  String SHARDED_DBOPS_APP_NAME = "StackGresShardedDbOps";
  String SHARDED_DBOPS_NAME_KEY = "sharded-dbops-name";
  String SHARDED_DBOPS_UID_KEY = "sharded-dbops-uid";
  String SHARDED_DBOPS_NAMESPACE_KEY = "sharded-dbops-namespace";
  String STREAM_KEY = "stream";
  String STREAM_APP_NAME = "StackGresStream";
  String STREAM_NAME_KEY = "stream-name";
  String STREAM_UID_KEY = "stream-uid";
  String STREAM_NAMESPACE_KEY = "stream-namespace";
  String CONFIG_APP_NAME = "StackGresConfig";
  String CONFIG_NAME_KEY = "config-name";
  String CONFIG_UID_KEY = "config-uid";
  String CONFIG_NAMESPACE_KEY = "config-namespace";

  String REST_APIUSER_KEY = "apiUsername";
  String REST_K8SUSER_KEY = "k8sUsername";
  String REST_PASSWORD_KEY = "password";

  String VERSION_KEY = STACKGRES_KEY_PREFIX + "operatorVersion";
  String RECONCILIATION_PAUSE_KEY = STACKGRES_KEY_PREFIX + "reconciliation-pause";
  String CLUSTER_CONTROLLER_VERSION_KEY = STACKGRES_KEY_PREFIX + "cluster-controller-version";
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

  String LOCK_SERVICE_ACCOUNT_KEY = STACKGRES_KEY_PREFIX + "lockServiceAccount";
  String LOCK_POD_KEY = STACKGRES_KEY_PREFIX + "lockPod";
  String LOCK_TIMEOUT_KEY = STACKGRES_KEY_PREFIX + "lockTimeout";

}
