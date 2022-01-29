/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

public enum OperatorProperty implements StackGresPropertyReader {

  OPERATOR_NAME("stackgres.operatorName"),
  OPERATOR_NAMESPACE("stackgres.operatorNamespace"),
  OPERATOR_IP("stackgres.operatorIP"),
  PROMETHEUS_AUTOBIND("stackgres.prometheus.allowAutobind"),
  GRAFANA_EMBEDDED("stackgres.prometheus.grafanaEmbedded"),
  AUTHENTICATION_SECRET_NAME("stackgres.authentication.secretName"),
  USE_ARBITRARY_USER("stackgres.useArbitraryUser"),
  EXTENSIONS_REPOSITORY_URLS("stackgres.extensionsRepositoryUrls"),
  CONFLICT_SLEEP_SECONDS("stackgres.conflictSleepSeconds"),
  LOCK_POLL_INTERVAL("stackgres.lockPollInterval"),
  LOCK_TIMEOUT("stackgres.lockTimeout");

  private static final Properties APPLICATION_PROPERTIES =
      StackGresPropertyReader.readApplicationProperties(OperatorProperty.class);

  private final String propertyName;

  OperatorProperty(String propertyName) {
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
