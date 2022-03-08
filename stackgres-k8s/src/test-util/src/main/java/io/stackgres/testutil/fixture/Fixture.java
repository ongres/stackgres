/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil.fixture;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.tukaani.xz.XZInputStream;

public abstract class Fixture<T> implements JsonFixture {

  private static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .build();

  private final Class<T> clazz;

  protected T fixture;

  @SuppressWarnings("unchecked")
  protected Fixture() {
    this.clazz = (Class<T>) getBaseGenericType(getClass().getGenericSuperclass());
    if (clazz.getPackage().getName().startsWith("io.stackgres")
        && clazz.getAnnotation(RegisterForReflection.class) == null) {
      throw new IllegalStateException("class " + clazz.getName() + " must have the annotation: "
          + RegisterForReflection.class);
    }
    if (clazz.isAssignableFrom(ObjectNode.class)) {
      fixture = (T) JSON_MAPPER.createObjectNode();
    } else if (clazz.isAssignableFrom(ArrayNode.class)) {
      fixture = (T) JSON_MAPPER.createArrayNode();
    } else {
      try {
        fixture = clazz.getConstructor().newInstance();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public T get() {
    return fixture;
  }

  public T readFromJson(String resource) {
    return readAnyFromJson(resource, this::readObjectFromJson);
  }

  public <E, R extends List<E>> R readListFromJson(String resource) {
    return readAnyFromJson(resource, this::readListOfObjectsFromJson);
  }

  public <J extends JsonNode> J readFromJsonXzAsJson(String resource) {
    return readAnyFromJson(resource, this::readJsonFromXz);
  }

  public <J extends JsonNode> J readFromJsonAsJson(String resource) {
    return readAnyFromJson(resource, this::readJson);
  }

  private <R> R readAnyFromJson(String resource, Function<InputStream, R> transformer) {
    Objects.requireNonNull(resource, "resource");
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("resource " + resource + " not found");
      }
      return transformer.apply(is);
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (IOException | RuntimeException ex) {
      throw new IllegalArgumentException("could not open resource " + resource, ex);
    }
  }

  private T readObjectFromJson(InputStream is) {
    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return JSON_MAPPER.readValue(CharStreams.toString(reader), clazz);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private <E, R extends List<E>> R readListOfObjectsFromJson(InputStream is) {
    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      JavaType type = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
      return JSON_MAPPER.readValue(CharStreams.toString(reader), type);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  private <J extends JsonNode> J readJsonFromXz(InputStream is) {
    try (XZInputStream xzIs = new XZInputStream(is)) {
      return (J) JSON_MAPPER.readTree(xzIs);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  private <J extends JsonNode> J readJson(InputStream is) {
    try {
      return (J) JSON_MAPPER.readTree(is);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Type getBaseGenericType(Type type) {
    Type genericType = Optional.of(type)
        .map(ParameterizedType.class::cast)
        .map(ParameterizedType::getActualTypeArguments)
        .map(arguments -> arguments[0])
        .orElseThrow();
    if (genericType instanceof ParameterizedType) {
      return getBaseGenericType(genericType);
    }
    return genericType;
  }

}
