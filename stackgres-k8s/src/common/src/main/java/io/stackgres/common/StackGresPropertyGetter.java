/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;
import java.util.Properties;

import org.jooq.lambda.Seq;

public interface StackGresPropertyGetter {

  String getEnvironmentVariableName();

  String getPropertyName();

  Properties getApplicationProperties();

  default boolean getBoolean() {
    return get().map(Boolean::parseBoolean).orElse(false);
  }

  default String getString() {
    return get()
        .orElseThrow(() -> new RuntimeException(
            "System property " + getPropertyName()
            + "and environment variable " + getEnvironmentVariableName()
            + " can not be found"));
  }

  default Optional<String> get() {
    return Seq.of(
        System.getProperty(getPropertyName()),
        System.getenv(getEnvironmentVariableName()),
        getApplicationProperties().getProperty(getPropertyName()))
        .filter(v -> v != null)
        .findFirst();
  }

  static Properties readApplicationProperties(Class<?> clazz) throws Exception {
    Properties properties = new Properties();
    properties.load(clazz.getResourceAsStream("/application.properties"));
    return properties;
  }

}
