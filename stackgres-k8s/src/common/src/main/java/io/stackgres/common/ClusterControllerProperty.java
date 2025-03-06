/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

public enum ClusterControllerProperty implements StackGresPropertyReader {

  CLUSTER_NAMESPACE("stackgres.clusterNamespace"),
  CLUSTER_NAME("stackgres.clusterName"),
  CLUSTER_CONTROLLER_POD_NAME("stackgres.clusterControllerPodName"),
  CLUSTER_CONTROLLER_POD_IP("stackgres.clusterControllerPodIp"),
  CLUSTER_CONTROLLER_NODE_NAME("stackgres.clusterControllerNodeName"),
  CLUSTER_CONTROLLER_EXTENSIONS_REPOSITORY_URLS(
      "stackgres.clusterControllerExtensionsRepositoryUrls"),
  CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES(
      "stackgres.clusterSkipOverwriteSharedLibraries"),
  CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER(
      "stackgres.clusterReconcilePgBouncer"),
  CLUSTER_CONTROLLER_RECONCILE_PATRONI_LABELS(
      "stackgres.clusterReconcilePatroniLabels"),
  CLUSTER_CONTROLLER_RECONCILE_PATRONI(
      "stackgres.clusterReconcilePatroni"),
  CLUSTER_CONTROLLER_RECONCILE_MANAGED_SQL(
      "stackgres.clusterReconcileManagedSql"),
  CLUSTER_CONTROLLER_RECONCILE_PATRONI_AFTER_MAJOR_VERSION_UPGRADE(
      "stackgres.clusterReconcilePatroniAfterMajorVersionUpgrade"),
  CLUSTER_CONTROLLER_JMX_COLLECTOR_YAML_CONFIG(
      "stackgres.cluster.controllerJmxCollectorYamlConfig");

  private static final Properties APPLICATION_PROPERTIES =
      StackGresPropertyReader.readApplicationProperties(ClusterControllerProperty.class);

  private final String propertyName;

  ClusterControllerProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  @Override
  public String getEnvironmentVariableName() {
    return name();
  }

  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public Properties getApplicationProperties() {
    return APPLICATION_PROPERTIES;
  }
}
