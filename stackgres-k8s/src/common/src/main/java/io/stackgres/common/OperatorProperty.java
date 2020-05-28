/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public enum OperatorProperty {
  OPERATOR_NAME("stackgres.operatorName"),
  OPERATOR_NAMESPACE("stackgres.operatorNamespace"),
  OPERATOR_IP("stackgres.operatorIP"),
  PROMETHEUS_AUTOBIND("stackgres.prometheus.allowAutobind"),
  GRAFANA_EMBEDDED("stackgres.prometheus.grafanaEmbedded"),
  AUTHENTICATION_SECRET_NAME("stackgres.authentication.secretName");

  private final String systemProperty;

  OperatorProperty(String systemProperty) {
    this.systemProperty = systemProperty;
  }

  public String property() {
    return name();
  }

  public String systemProperty() {
    return systemProperty;
  }
}
