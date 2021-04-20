/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class JsonUtil {

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private JsonUtil() {}

  public static <T> T readFromJson(String resource, Class<T> clazz) {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(clazz, "clazz");
    if (clazz.getPackage().getName().startsWith("io.stackgres")
        && clazz.getAnnotation(RegisterForReflection.class) == null) {
      throw new IllegalStateException("class " + clazz.getName() + " must have the annotation: "
          + RegisterForReflection.class);
    }
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("resource " + resource + " not found");
      }
      try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        return JSON_MAPPER.readValue(CharStreams.toString(reader), clazz);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("could not open resource " + resource, e);
    }
  }

  public static <T> List<T> readListFromJson(String resource, Class<T> clazz) {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(clazz, "clazz");
    if (clazz.getPackage().getName().startsWith("io.stackgres")
        && clazz.getAnnotation(RegisterForReflection.class) == null) {
      throw new IllegalStateException("class " + clazz.getName() + " must have the annotation: "
          + RegisterForReflection.class);
    }
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("resource " + resource + " not found");
      }
      try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        JavaType type = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
        return JSON_MAPPER.readValue(CharStreams.toString(reader), type);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("could not open resource " + resource, e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends JsonNode> T readFromJsonAsJson(String resource) {
    Objects.requireNonNull(resource, "resource");
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("resource " + resource + " not found");
      }
      try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        return (T) JSON_MAPPER.readTree(CharStreams.toString(reader));
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("could not open resource " + resource, e);
    }
  }

}
