/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonObject implements Map<String, Object> {

  private final Map<String, Object> map;

  public JsonObject() {
    this.map = new HashMap<>();
  }

  public JsonObject(Map<String, Object> map) {
    this.map = map;
  }

  public boolean hasObject(String key) {
    Object value = get(key);
    return value != null && value instanceof Map;
  }

  public boolean hasWritableObject(String key) {
    Object value = get(key);
    return value != null && value instanceof HashMap;
  }

  public JsonObject getObject(String key) {
    Object value = map.get(key);
    return toObject(value);
  }

  public JsonObject getObjectOrPut(String key) {
    Object value = map.get(key);
    return toObject(value, key);
  }

  public Stream<Map.Entry<String, JsonObject>> streamObjectEntries() {
    return entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), toObject(entry.getValue())));
  }

  private JsonObject toObject(Object value) {
    return toObject(value, null);
  }

  @SuppressWarnings("unchecked")
  private JsonObject toObject(Object value, String setToKey) {
    final JsonObject objectValue;
    if (value instanceof JsonObject) {
      objectValue = (JsonObject) value;
    } else if (value instanceof Map) {
      objectValue = new JsonObject((Map<String, Object>) value);
      if (setToKey != null) {
        map.put(setToKey, objectValue);
      }
    } else if (value == null) {
      objectValue = new JsonObject();
      if (setToKey != null) {
        map.put(setToKey, objectValue);
      }
    } else {
      throw new ClassCastException("Can not cast " + value.getClass().getName() + " to Map");
    }
    return objectValue;
  }

  public JsonArray getArray(String key) {
    Object value = map.get(key);
    return toArray(value);
  }

  public JsonArray getArrayOrPut(String key) {
    Object value = map.get(key);
    return toArray(value, key);
  }

  private JsonArray toArray(Object value) {
    return toArray(value, null);
  }

  @SuppressWarnings("unchecked")
  private JsonArray toArray(Object value, String setToKey) {
    final JsonArray arrayValue;
    if (value instanceof JsonArray) {
      arrayValue = (JsonArray) value;
    } else if (value instanceof List) {
      arrayValue = new JsonArray((List<Object>) value);
      if (setToKey != null) {
        map.put(setToKey, arrayValue);
      }
    } else if (value == null) {
      arrayValue = new JsonArray();
      if (setToKey != null) {
        map.put(setToKey, arrayValue);
      }
    } else {
      throw new ClassCastException("Can not cast " + value.getClass().getName() + " to List");
    }
    return arrayValue;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return map.get(key);
  }

  @Override
  public Object put(String key, Object value) {
    return map.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<String> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<Object> values() {
    return map.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return map.entrySet();
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return map.equals(obj);
  }

}
