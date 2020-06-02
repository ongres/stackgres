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
import io.stackgres.common.StackGresUtil;

@Singleton
public class OperatorContext implements ConfigContext {

  private static final ImmutableMap<OperatorProperty, String> DEFAULT_CONTEXT = ImmutableMap
      .<OperatorProperty, String>builder()
      .put(OperatorProperty.OPERATOR_NAMESPACE, StackGresUtil.OPERATOR_NAMESPACE)
      .put(OperatorProperty.OPERATOR_NAME, StackGresUtil.OPERATOR_NAME)
      .put(OperatorProperty.PROMETHEUS_AUTOBIND, StackGresUtil.PROMETHEUS_AUTOBIND)
      .put(OperatorProperty.OPERATOR_IP, StackGresUtil.OPERATOR_IP)
      .put(OperatorProperty.AUTHENTICATION_SECRET_NAME, StackGresUtil.AUTHENTICATION_SECRET_NAME)
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
