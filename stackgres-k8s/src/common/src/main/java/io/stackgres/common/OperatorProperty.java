/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

import org.jooq.lambda.Unchecked;

public enum OperatorProperty implements StackGresPropertyReader {

  OPERATOR_NAME("stackgres.operatorName"),
  OPERATOR_NAMESPACE("stackgres.operatorNamespace"),
  OPERATOR_IP("stackgres.operatorIP"),
  PROMETHEUS_AUTOBIND("stackgres.prometheus.allowAutobind"),
  GRAFANA_EMBEDDED("stackgres.prometheus.grafanaEmbedded"),
  AUTHENTICATION_SECRET_NAME("stackgres.authentication.secretName"),
  USE_ARBITRARY_USER("stackgres.useArbitraryUser");

  private static final Properties APPLICATION_PROPERTIES =
      Unchecked.supplier(() -> StackGresPropertyReader
          .readApplicationProperties(OperatorProperty.class)).get();

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
