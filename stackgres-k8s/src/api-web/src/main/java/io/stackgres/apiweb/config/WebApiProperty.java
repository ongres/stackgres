/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.config;

public enum WebApiProperty {

  RESTAPI_NAMESPACE("stackgres.restapiNamespace"),
  GRAFANA_EMBEDDED("stackgres.prometheus.grafanaEmbedded");

  private final String systemProperty;

  WebApiProperty(String systemProperty) {
    this.systemProperty = systemProperty;
  }

  public String property() {
    return name();
  }

  public String systemProperty() {
    return systemProperty;
  }
}
