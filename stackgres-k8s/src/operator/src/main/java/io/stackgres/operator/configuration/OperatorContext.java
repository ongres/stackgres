/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.configuration;

import java.util.Optional;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.ConfigContext;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.common.OperatorConfigDefaults;

@Singleton
public class OperatorContext implements ConfigContext<OperatorProperty> {

  private static final ImmutableMap<OperatorProperty, String> DEFAULT_CONTEXT = ImmutableMap
      .<OperatorProperty, String>builder()
      .put(OperatorProperty.OPERATOR_NAMESPACE, OperatorConfigDefaults.OPERATOR_NAMESPACE)
      .put(OperatorProperty.OPERATOR_NAME, OperatorConfigDefaults.OPERATOR_NAME)
      .put(OperatorProperty.PROMETHEUS_AUTOBIND, OperatorConfigDefaults.PROMETHEUS_AUTOBIND)
      .put(OperatorProperty.OPERATOR_IP, OperatorConfigDefaults.OPERATOR_IP)
      .build();

  @Override
  public Optional<String> getProperty(OperatorProperty configProperty) {

    Optional<String> envProp = Optional.ofNullable(System.getenv(configProperty.property()));
    if (envProp.isPresent()) {
      return envProp;
    } else {
      return Optional.ofNullable(DEFAULT_CONTEXT.get(configProperty));
    }

  }

  @Override
  public String get(OperatorProperty configProperty) {
    return Optional.ofNullable(System.getenv(configProperty.property()))
        .orElseGet(() -> DEFAULT_CONTEXT.get(configProperty));
  }

  @Override
  public boolean getAsBoolean(OperatorProperty configProperty) {
    return Boolean.parseBoolean(get(configProperty));
  }

}
