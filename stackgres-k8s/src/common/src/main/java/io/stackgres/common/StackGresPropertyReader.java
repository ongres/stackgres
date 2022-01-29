/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.jooq.lambda.Seq;

/**
 * Represents a property which value is the first one read from system properties, environment
 * variables and application properties (in this order).
 */
public interface StackGresPropertyReader {

  /**
   * The name of the environment variable used to retrieve this property value.
   */
  String getEnvironmentVariableName();

  /**
   * The name of the property used to retrieve this property value from system properties or
   * application properties.
   */
  String getPropertyName();

  /**
   * Implementation of this interface must implement caching of the {@code Properties} instance
   * returned by this method.
   */
  Properties getApplicationProperties();

  /**
   * Return true only if first existing value of associated system property, environment variable or
   * application property (in this exact sequence) is true after calling method
   * {@code Boolean.parseBoolean(String)}. Otherwise false is returned.
   */
  default boolean getBoolean() {
    return get().map(Boolean::parseBoolean).orElse(false);
  }

  /**
   * Return first existing value of associated system property, environment variable or application
   * property (in this exact sequence). Otherwise throw a {@code RuntimeException}.
   */
  default String getString() {
    return get()
        .orElseThrow(() -> new RuntimeException(
            "Neither system property '" + getPropertyName()
                + "', nor environment variable '" + getEnvironmentVariableName()
                + "' are set"));
  }

  /**
   * Return first existing value of associated system property, environment variable or application
   * property (in this exact sequence). Otherwise throw a {@code RuntimeException}.
   */
  default int getInt() {
    return get()
        .map(Integer::parseInt)
        .orElseThrow(() -> new RuntimeException(
            "Neither system property '" + getPropertyName()
                + "', nor environment variable '" + getEnvironmentVariableName()
                + "' are set"));
  }

  /**
   * Return first existing value of associated system property, environment variable or application
   * property (in this exact sequence) as an array by splitting string using comma character ",".
   * If the value is empty it returns the an empty array. Otherwise throw a
   * {@code RuntimeException}.
   */
  default String[] getStringArray() {
    if (getString().isEmpty()) {
      return new String[0];
    }
    return getString().split(",");
  }

  /**
   * Return first existing value of associated system property, environment variable or application
   * property (in this exact sequence).
   */
  default Optional<String> get() {
    return Seq.of(
        System.getProperty(getPropertyName()),
        System.getenv(getEnvironmentVariableName()),
        getApplicationProperties().getProperty(getPropertyName()))
        .filter(Objects::nonNull)
        .findFirst();
  }

  /**
   * Implementation of this interface should use this function to read the application properties
   * file.
   */
  static Properties readApplicationProperties(Class<?> clazz) {
    Properties properties = new Properties();
    try (InputStream is = clazz.getResourceAsStream("/application.properties")) {
      properties.load(is);
    } catch (IOException e) {
      throw new UncheckedIOException("Can't read properties file", e);
    }
    return properties;
  }

}
