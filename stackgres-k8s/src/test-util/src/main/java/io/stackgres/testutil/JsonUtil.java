/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import junit.framework.AssertionFailedError;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class JsonUtil {

  private static final JsonMapper JSON_MAPPER = createJsonMapper();

  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
      justification = "false positive")
  private static JsonMapper createJsonMapper() {
    return JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .build();
  }

  public static JsonMapper jsonMapper() {
    return JSON_MAPPER;
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

  public static void assertJsonEquals(JsonNode expected, JsonNode actual, String message) {
    assertJsonEquals(expected.toString(), actual.toString(), message);
  }

  public static void assertJsonEquals(String expected, String actual) {
    assertJsonEquals(expected, actual, null);
  }

  public static void assertJsonEquals(String expected, String actual, String message) {
    try {
      JSONAssert.assertEquals(
          expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    } catch (AssertionError ex) {
      if (message != null) {
        throw new AssertionFailedError(message + "\n\n" + ex.getMessage());
      }
      throw new AssertionFailedError(ex.getMessage());
    }
  }

  private JsonUtil() {
  }

}
