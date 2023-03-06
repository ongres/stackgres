/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonArray extends ArrayList<Object> {

  private static final long serialVersionUID = 1L;

  public JsonArray() {
    super();
  }

  public JsonArray(Collection<? extends Object> l) {
    super(l);
  }

  public JsonArray(int initialCapacity) {
    super(initialCapacity);
  }

  @SuppressWarnings("unchecked")
  public Stream<JsonObject> streamObjects() {
    return stream()
        .map(e -> (Map<? extends String, ? extends Object>) e)
        .map(JsonObject::new);
  }

  public List<JsonObject> objects() {
    return streamObjects()
        .toList();
  }

  public Stream<String> streamStrings() {
    return stream()
        .map(String.class::cast);
  }

  public List<String> strings() {
    return streamStrings()
        .toList();
  }

}
