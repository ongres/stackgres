/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

import org.jooq.lambda.Unchecked;

public enum ClusterControllerProperty implements StackGresPropertyReader {

  CLUSTER_NAMESPACE("stackgres.clusterNamespace"),
  CLUSTER_NAME("stackgres.clusterName"),
  CLUSTER_CONTROLLER_POD_NAME("stackgres.clusterControllerPodName"),
  CLUSTER_CONTROLLER_EXTENSIONS_REPOSITORY_URLS(
      "stackgres.clusterControllerExtensionsRepositoryUrls"),
  CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES(
      "stackgres.clusterSkipOverwriteSharedLibraries"),
  CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER(
      "stackgres.clusterSkipReconcilePgBouncer");

  private static final Properties APPLICATION_PROPERTIES =
      Unchecked.supplier(() -> StackGresPropertyReader
          .readApplicationProperties(ClusterControllerProperty.class)).get();

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
