/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;

@Singleton
public class ConfigLoader implements ConfigContext {

  private static final ImmutableMap<ConfigProperty, String> DEFAULT_CONTEXT = ImmutableMap
      .<ConfigProperty, String>builder()
      .put(ConfigProperty.CONTAINER_BUILD, StackGresUtil.CONTAINER_BUILD)
      .put(ConfigProperty.CRD_GROUP, StackGresUtil.CRD_GROUP)
      .put(ConfigProperty.CRD_VERSION, StackGresUtil.CRD_VERSION)
      .put(ConfigProperty.OPERATOR_NAMESPACE, StackGresUtil.OPERATOR_NAMESPACE)
      .put(ConfigProperty.OPERATOR_NAME, StackGresUtil.OPERATOR_NAME)
      .put(ConfigProperty.OPERATOR_VERSION, StackGresUtil.OPERATOR_VERSION)
      .put(ConfigProperty.PROMETHEUS_AUTOBIND, StackGresUtil.PROMETHEUS_AUTOBIND)
      .put(ConfigProperty.OPERATOR_IP, StackGresUtil.OPERATOR_IP)
      .put(ConfigProperty.DOCUMENTATION_URI, StackGresUtil.DOCUMENTATION_URI)
      .put(ConfigProperty.DOCUMENTATION_ERRORS_PATH, StackGresUtil.DOCUMENTATION_ERRORS_PATH)
      .build();

  @Override
  public Optional<String> getProperty(ConfigProperty configProperty) {

    Optional<String> envProp = Optional.ofNullable(System.getenv(configProperty.property()));
    if (envProp.isPresent()) {
      return envProp;
    } else {
      return Optional.ofNullable(DEFAULT_CONTEXT.get(configProperty));
    }

  }

  @Override
  public String get(ConfigProperty configProperty) {
    return Optional.ofNullable(System.getenv(configProperty.property()))
        .orElseGet(() -> DEFAULT_CONTEXT.get(configProperty));
  }

}
