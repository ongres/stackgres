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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.CharStreams;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.tukaani.xz.XZInputStream;

public class JsonUtil {

  public static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private JsonUtil() {
    JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    JSON_MAPPER.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  }

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
  public static <T extends JsonNode> T readFromJsonXzAsJson(String resource) {
    Objects.requireNonNull(resource, "resource");
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("resource " + resource + " not found");
      }
      try (XZInputStream xzIs = new XZInputStream(is)) {
        try (Reader reader = new InputStreamReader(xzIs, StandardCharsets.UTF_8)) {
          return (T) JSON_MAPPER.readTree(CharStreams.toString(reader));
        }
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

  public static JsonNode toJson(Object object) {
    return JSON_MAPPER.valueToTree(object);
  }

  public static <T> T toJson(String content, Class<T> clazz) {
    try {
      return JSON_MAPPER.readValue(content, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("invalid json content", e);
    }

  }

  public static <T> T fromJson(TreeNode json, Class<T> clazz) {
    try {
      return JSON_MAPPER.treeToValue(json, clazz);
    } catch (IOException e) {
      throw new IllegalArgumentException("could not convert JSON to " + clazz + "\n\n" + json, e);
    }
  }

  public static void assertJsonEquals(JsonNode expected, JsonNode actual) {
    assertJsonEquals(expected.toString(), actual.toString());
  }

  public static void assertJsonEquals(String expected, String actual) {
    try {
      JSONAssert.assertEquals(
          expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

}
