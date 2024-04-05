/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import junit.framework.AssertionFailedError;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

public class JsonUtil {

  private static final JsonMapper JSON_MAPPER = createJsonMapper();

  private static JsonMapper createJsonMapper() {
    return JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .build();
  }

  private static final YAMLMapper YAML_MAPPER = createYamlMapper();

  private static YAMLMapper createYamlMapper() {
    return YAMLMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .build();
  }

  public static JsonMapper jsonMapper() {
    return JSON_MAPPER;
  }

  public static YAMLMapper yamlMapper() {
    return YAML_MAPPER;
  }

  public static JsonNode toJson(Object object) {
    return JSON_MAPPER.valueToTree(object);
  }

  public static <T> T fromJson(String content, Class<T> clazz) {
    try {
      return JSON_MAPPER.readValue(content, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("invalid json content", e);
    }

  }

  public static <T> T fromJson(JsonNode json, Class<T> clazz) {
    try {
      return JSON_MAPPER.readValue(json.toString(), clazz);
    } catch (IOException e) {
      throw new IllegalArgumentException("could not convert JSON to " + clazz + "\n\n" + json, e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T copy(T object) {
    return (T) fromJson(toJson(object), object.getClass());
  }

  public static <T> T convert(Object original,
      Class<T> clazz) {
    return JSON_MAPPER.convertValue(original, clazz);
  }

  public static void assertJsonEquals(JsonNode expected, JsonNode actual) {
    assertJsonEquals(expected, actual, null);
  }

  public static void assertJsonEquals(String expected, String actual, String message) {
    assertJsonEquals(
        Optional.ofNullable(expected)
        .map(Unchecked.function(JSON_MAPPER::readTree))
        .orElse(null),
        Optional.ofNullable(actual)
        .map(Unchecked.function(JSON_MAPPER::readTree))
        .orElse(null),
        message);
  }

  public static void assertJsonEquals(String expected, String actual) {
    assertJsonEquals(expected, actual, null);
  }

  public static void assertJsonEquals(JsonNode expected, JsonNode actual, String message) {
    try {
      assertAnyJsonEquals(JsonPointer.empty(), expected, actual);
    } catch (AssertionError ex) {
      if (message != null) {
        throw new AssertionFailedError(message + "\n\n" + ex.getMessage());
      }
      throw new AssertionFailedError(ex.getMessage());
    }
  }

  @SuppressWarnings("resource")
  private static void assertJsonEquals(JsonPointer pointer, ObjectNode expected, ObjectNode actual) {
    for (var expectedField : Seq.seq(expected.fields()).toList()) {
      if (!actual.has(expectedField.getKey())) {
        throw new AssertionFailedError(
            "At pointer " + pointer
            + " actual has no expected field " + expectedField.getKey());
      }
      assertAnyJsonEquals(
          pointer.appendProperty(expectedField.getKey()),
          expectedField.getValue(),
          actual.get(expectedField.getKey()));
    }
    for (var actualField : Seq.seq(actual.fields()).toList()) {
      if (!expected.has(actualField.getKey())) {
        throw new AssertionFailedError(
            "At pointer " + pointer
            + " actual has unexpected field " + actualField.getKey());
      }
    }
  }

  @SuppressWarnings("resource")
  private static void assertJsonEquals(JsonPointer pointer, ArrayNode expected, ArrayNode actual) {
    if (expected.size() != actual.size()) {
      throw new AssertionFailedError(
          "At pointer " + pointer
          + " expected an array of " + expected.size() + " elements"
          + " but was of " + actual.size() + " elements ");
    }
    int index = 0;
    for (var expectedElement : Seq.seq(expected.elements()).toList()) {
      if (!actual.has(index)) {
        throw new AssertionFailedError(
            "At pointer " + pointer
            + " actual has no expected element " + index);
      }
      assertAnyJsonEquals(
          pointer.appendIndex(index),
          expectedElement,
          actual.get(index));
      index++;
    }
  }

  public static void assertAnyJsonEquals(JsonPointer pointer, JsonNode expected, JsonNode actual) {
    JsonNodeType expectedType = Optional.ofNullable(expected).map(JsonNode::getNodeType).orElse(null);
    JsonNodeType actualType = Optional.ofNullable(actual).map(JsonNode::getNodeType).orElse(null);
    if (!Objects.equals(expectedType, actualType)) {
      throw new AssertionFailedError(
          "At pointer " + pointer
          + " expected " + expectedType
          + " but was " + actualType);
    }
    if (expected instanceof ObjectNode expectedObject
        && actual instanceof ObjectNode actualObject) {
      assertJsonEquals(pointer, expectedObject, actualObject);
    }
    if (expected instanceof ArrayNode expectedArray
        && actual instanceof ArrayNode actualArray) {
      assertJsonEquals(pointer, expectedArray, actualArray);
    }
    if (!Objects.equals(expected, actual)) {
      throw new AssertionFailedError(
          "At pointer " + pointer
          + " expected " + expected
          + " but was " + actual);
    }
  }

  public static void sortArray(JsonNode jsonNode) {
    if (jsonNode instanceof ArrayNode arrayNode) {
      if (Seq.seq(arrayNode.elements()).allMatch(TextNode.class::isInstance)) {
        var elements = Seq.seq(arrayNode.elements())
            .sorted(Comparator.comparing(element -> TextNode.class.cast(element).asText()))
            .toList();
        arrayNode.removeAll();
        arrayNode.addAll(elements);
        return;
      }
      throw new RuntimeException("ArrayNode do not container only TextNode elements");
    }
    throw new RuntimeException("Class " + jsonNode.getClass().getSimpleName() + " is not instance of ArrayNode");
  }

  private JsonUtil() {
  }

}
