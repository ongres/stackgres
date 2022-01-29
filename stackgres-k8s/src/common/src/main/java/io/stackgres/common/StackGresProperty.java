/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

public enum StackGresProperty implements StackGresPropertyReader {

  OPERATOR_VERSION("stackgres.operatorVersion"),
  OPERATOR_IMAGE_VERSION("stackgres.operatorImageVersion"),
  DOCUMENTATION_URI("stackgres.documentation.uri"),
  DOCUMENTATION_ERRORS_PATH("stackgres.documentation.errorsPath"),
  SG_CONTAINER_REGISTRY("stackgres.containerRegistry"),
  SG_IMAGE_PULL_POLICY("stackgres.imagePullPolicy"),
  SG_IMAGE_PATRONI("stackgres.imagePatroni"),
  SG_IMAGE_POSTGRES_UTIL("stackgres.imagePostgresUtil"),
  SG_IMAGE_PGBOUNCER("stackgres.imagePgbouncer"),
  SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER("stackgres.imagePrometheusPostgresExporter"),
  SG_IMAGE_ENVOY("stackgres.imageEnvoy"),
  SG_IMAGE_FLUENT_BIT("stackgres.imageFluentBit"),
  SG_IMAGE_FLUENTD("stackgres.imageFluentd"),
  SG_IMAGE_CLUSTER_CONTROLLER("stackgres.imageClusterController"),
  SG_IMAGE_DISTRIBUTEDLOGS_CONTROLLER("stackgres.imageDistributedlogsController"),
  SG_IMAGE_KUBECTL("stackgres.imageKubectl");

  private static final Properties APPLICATION_PROPERTIES =
      StackGresPropertyReader.readApplicationProperties(StackGresProperty.class);

  private final String propertyName;

  StackGresProperty(String propertyName) {
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
