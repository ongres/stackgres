/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonObject extends HashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  public JsonObject() {
    super();
  }

  public JsonObject(Map<? extends String, ? extends Object> m) {
    super(m);
  }

  public JsonObject(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public JsonObject(int initialCapacity) {
    super(initialCapacity);
  }

  public boolean hasObject(String key) {
    Object value = get(key);
    return value != null && value instanceof Map;
  }

  @SuppressWarnings("unchecked")
  public JsonObject getObject(String key) {
    return new JsonObject((Map<? extends String, ? extends Object>) get(key));
  }

  @SuppressWarnings("unchecked")
  public JsonArray getArray(String key) {
    return new JsonArray((List<? extends Object>) get(key));
  }

}
