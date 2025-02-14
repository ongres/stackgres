/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.configuration;

import java.util.Properties;

import io.stackgres.common.StackGresPropertyReader;

public enum WebApiProperty implements StackGresPropertyReader {

  RESTAPI_NAMESPACE("stackgres.restapiNamespace"),
  GRAFANA_EMBEDDED("stackgres.prometheus.grafanaEmbedded"),
  EXTENSIONS_REPOSITORY_URLS("stackgres.extensionsRepositoryUrls"),
  RESTAPI_JMX_COLLECTOR_YAML_CONFIG("stackgres.restapi.jmxCollectorYamlConfig");

  private static final Properties APPLICATION_PROPERTIES =
      StackGresPropertyReader.readApplicationProperties(WebApiProperty.class);

  private final String propertyName;

  WebApiProperty(String propertyName) {
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
