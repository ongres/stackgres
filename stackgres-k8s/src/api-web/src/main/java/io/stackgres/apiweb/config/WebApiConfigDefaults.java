/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.config;

import java.util.Properties;

public enum WebApiConfigDefaults {

  INSTANCE;

  public static final String RESTAPI_NAMESPACE = INSTANCE.restApiNamespace;

  public static final String GRAFANA_EMBEDDED = INSTANCE.grafanaEmbedded;

  private final String restApiNamespace;

  private final String grafanaEmbedded;

  WebApiConfigDefaults() {
    try {
      Properties properties = new Properties();
      properties.load(WebApiConfigDefaults.class.getResourceAsStream("/application.properties"));
      restApiNamespace = properties.getProperty(WebApiProperty.RESTAPI_NAMESPACE.systemProperty());
      grafanaEmbedded = properties.getProperty(WebApiProperty.GRAFANA_EMBEDDED.systemProperty());

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
