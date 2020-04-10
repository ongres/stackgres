/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

public enum ConfigProperty {
  OPERATOR_NAME("stackgres.operatorName"),
  OPERATOR_NAMESPACE("stackgres.operatorNamespace"),
  OPERATOR_IP("stackgres.operatorIP"),
  OPERATOR_VERSION("stackgres.operatorVersion"),
  PROMETHEUS_AUTOBIND("stackgres.prometheus.allowAutobind"),
  GRAFANA_EMBEDDED("stackgres.prometheus.grafanaEmbedded"),
  DOCUMENTATION_URI("stackgres.documentation.uri"),
  DOCUMENTATION_ERRORS_PATH("stackgres.documentation.errorsPath"),
  AUTHENTICATION_SECRET_NAME("stackgres.authentication.secretName");

  private final String systemProperty;

  ConfigProperty(String systemProperty) {
    this.systemProperty = systemProperty;
  }

  public String property() {
    return name();
  }

  public String systemProperty() {
    return systemProperty;
  }
}
