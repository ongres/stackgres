/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public interface StackGresContext {

  String APP_KEY = "app";
  String APP_NAME = "StackGresCluster";
  String CLUSTER_NAME_KEY = "cluster-name";
  String CLUSTER_UID_KEY = "cluster-uid";
  String CLUSTER_NAMESPACE_KEY = "cluster-namespace";
  String RIGHT_VALUE = Boolean.TRUE.toString();
  String WRONG_VALUE = Boolean.FALSE.toString();
  String CLUSTER_KEY = "cluster";
  String BACKUP_KEY = "backup";
  String DB_OPS_KEY = "db-ops";
  String SCHEDULED_BACKUP_KEY = "scheduled-backup";
  String DISRUPTIBLE_KEY = "disruptible";
  String ROLE_KEY = "role";
  String PRIMARY_ROLE = "master";
  String REPLICA_ROLE = "replica";
  String PROMOTE_ROLE = "promote";
  String DEMOTE_ROLE = "demote";
  String UNINITIALIZED_ROLE = "uninitialized";
  String STANDBY_LEADER_ROLE = "standby_leader";
  String REST_APIUSER_KEY = "apiUsername";
  String REST_K8SUSER_KEY = "k8sUsername";
  String REST_PASSWORD_KEY = "password";
  String DISTRIBUTED_LOGS_APP_NAME = "StackGresDistributedLogs";
  String DISTRIBUTED_LOGS_CLUSTER_NAME_KEY = "distributed-logs-name";
  String DISTRIBUTED_LOGS_CLUSTER_NAMESPACE_KEY = "distributed-logs-namespace";
  String DISTRIBUTED_LOGS_CLUSTER_UID_KEY = "distributed-logs-uid";
  String DISTRIBUTED_LOGS_CLUSTER_KEY = "distributed-logs-cluster";
  String DISTRIBUTED_LOGS_BACKUP_KEY = "distributed-logs-backup";

  String STACKGRES_KEY_PREFIX = "stackgres.io/";
  String VERSION_KEY = STACKGRES_KEY_PREFIX + "operatorVersion";

  String AUTH_KEY = "api.stackgres.io/auth";
  String AUTH_USER_VALUE = "user";

  String KUBECTL_IMAGE = "bitnami/kubectl:1.19.2";
  String BUSYBOX_IMAGE = "busybox:1.31.1";

}
