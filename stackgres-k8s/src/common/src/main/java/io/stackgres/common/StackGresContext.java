/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

import com.google.common.base.Preconditions;

public enum StackGresContext {

  INSTANCE;

  public static final String APP_KEY = "app";
  public static final String APP_NAME = "StackGresCluster";
  public static final String CLUSTER_NAME_KEY = "cluster-name";
  public static final String CLUSTER_UID_KEY = "cluster-uid";
  public static final String CLUSTER_NAMESPACE_KEY = "cluster-namespace";
  public static final String RIGHT_VALUE = Boolean.TRUE.toString();
  public static final String WRONG_VALUE = Boolean.FALSE.toString();
  public static final String CLUSTER_KEY = "cluster";
  public static final String BACKUP_KEY = "backup";
  public static final String DISRUPTIBLE_KEY = "disruptible";
  public static final String ROLE_KEY = "role";
  public static final String PRIMARY_ROLE = "master";
  public static final String REPLICA_ROLE = "replica";
  public static final String PROMOTE_ROLE = "promote";
  public static final String DEMOTE_ROLE = "demote";
  public static final String UNINITIALIZED_ROLE = "uninitialized";
  public static final String STANDBY_LEADER_ROLE = "standby_leader";
  public static final String REST_APIUSER_KEY = "apiUsername";
  public static final String REST_K8SUSER_KEY = "k8sUsername";
  public static final String REST_PASSWORD_KEY = "password";
  public static final String DISTRIBUTED_LOGS_APP_NAME = "StackGresDistributedLogs";
  public static final String DISTRIBUTED_LOGS_CLUSTER_NAME_KEY = "distributed-logs-name";
  public static final String DISTRIBUTED_LOGS_CLUSTER_NAMESPACE_KEY = "distributed-logs-namespace";
  public static final String DISTRIBUTED_LOGS_CLUSTER_UID_KEY = "distributed-logs-uid";
  public static final String DISTRIBUTED_LOGS_CLUSTER_KEY = "distributed-logs-cluster";
  public static final String DISTRIBUTED_LOGS_BACKUP_KEY = "distributed-logs-backup";

  public static final String KUBECTL_IMAGE = "bitnami/kubectl:1.18.3";
  public static final String BUSYBOX_IMAGE = "busybox:1.31.1";

  public static final String CRD_GROUP = INSTANCE.group;
  public static final String CRD_VERSION = INSTANCE.version;

  public static final String CONTAINER_BUILD = INSTANCE.containerBuild;

  public static final String OPERATOR_VERSION = INSTANCE.operatorVersion;
  public static final String DOCUMENTATION_URI = INSTANCE.documentationUri;
  public static final String DOCUMENTATION_ERRORS_PATH = INSTANCE.documentationErrorsPath;

  private final String group;
  private final String version;

  private final String containerBuild;
  private final String operatorVersion;
  private final String documentationUri;
  private final String documentationErrorsPath;

  StackGresContext() {
    try {

      Properties properties = new Properties();
      properties.load(StackGresContext.class.getResourceAsStream("/application.properties"));

      group = getProperty(properties, StackGresProperty.CRD_GROUP);
      version = getProperty(properties, StackGresProperty.CRD_VERSION);
      containerBuild = getProperty(properties, StackGresProperty.CONTAINER_BUILD);
      operatorVersion = getProperty(properties, StackGresProperty.OPERATOR_VERSION);
      documentationUri = getProperty(properties, StackGresProperty.DOCUMENTATION_URI);
      documentationErrorsPath = getProperty(properties,
          StackGresProperty.DOCUMENTATION_ERRORS_PATH);
      Preconditions.checkNotNull(group);
      Preconditions.checkNotNull(version);
      Preconditions.checkNotNull(containerBuild);

    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

  }

  private static String getProperty(Properties properties, StackGresProperty configProperty) {
    return properties.getProperty(configProperty.systemProperty());
  }

}
