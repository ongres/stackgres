/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.config;

import java.util.Optional;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.ConfigContext;

@Singleton
public class WebApiContext implements ConfigContext<WebApiProperty> {

  private static final ImmutableMap<WebApiProperty, String> DEFAULT_CONTEXT = ImmutableMap
      .<WebApiProperty, String>builder()
      .put(WebApiProperty.GRAFANA_EMBEDDED, WebApiConfigDefaults.GRAFANA_EMBEDDED)
      .put(WebApiProperty.RESTAPI_NAMESPACE, WebApiConfigDefaults.RESTAPI_NAMESPACE)
      .build();

  @Override
  public Optional<String> getProperty(WebApiProperty configProperty) {
    Optional<String> envProp = Optional.ofNullable(System.getenv(configProperty.property()));
    if (envProp.isPresent()) {
      return envProp;
    } else {
      return Optional.ofNullable(DEFAULT_CONTEXT.get(configProperty));
    }
  }

  @Override
  public String get(WebApiProperty configProperty) {
    return Optional.ofNullable(System.getenv(configProperty.property()))
        .orElseGet(() -> DEFAULT_CONTEXT.get(configProperty));
  }

  @Override
  public boolean getAsBoolean(WebApiProperty configProperty) {
    return Boolean.parseBoolean(get(configProperty));
  }
}
