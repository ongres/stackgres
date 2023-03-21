/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.MoveOperation;
import com.github.fge.jsonpatch.RemoveOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;

public interface JsonPatchMutatorUtil {

  JsonNodeFactory FACTORY = JacksonUtils.nodeFactory();

  JsonPointer SPEC_POINTER = JsonPointer.of("spec");
  JsonPointer STATUS_POINTER = JsonPointer.of("status");

  default boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }

  default @NotNull String getJsonMappingField(@NotNull String field, @NotNull Class<?> clazz) {
    JsonProperty annotation = null;
    boolean fieldNotFound = true;
    Class<?> currentClazz = clazz;
    do {
      try {
        annotation = currentClazz.getDeclaredField(field).getAnnotation(JsonProperty.class);
        fieldNotFound = false;
        break;
      } catch (NoSuchFieldException ex) {
        currentClazz = currentClazz.getSuperclass();
      }
    } while (currentClazz != Object.class);
    if (fieldNotFound) {
      throw new RuntimeException("Filed " + field + " not found in class "
          + clazz + " or any of its subclasses");
    }
    return annotation != null ? annotation.value() : field;
  }

  default JsonPatchOperation buildAddOperation(JsonPointer path, String value) {
    return buildAddOperation(path, TextNode.valueOf(value));
  }

  default JsonPatchOperation buildAddOperation(JsonPointer path, JsonNode value) {
    return new AddOperation(path, value);
  }

  default JsonPatchOperation buildReplaceOperation(JsonPointer path, String value) {
    return buildReplaceOperation(path, TextNode.valueOf(value));
  }

  default JsonPatchOperation buildReplaceOperation(JsonPointer path, JsonNode value) {
    return new ReplaceOperation(path, value);
  }

  default List<JsonPatchOperation> applyDefaults(JsonPointer basePointer, JsonNode defaultNode,
      JsonNode incomingNode) {

    List<JsonPatchOperation> operations = new ArrayList<>();

    List<Tuple3<JsonNode, JsonPointer, JsonNode>> jsonNodeStack = new ArrayList<>();
    jsonNodeStack.add(Tuple.tuple(incomingNode, basePointer, defaultNode));
    while (!jsonNodeStack.isEmpty()) {
      List<Tuple3<JsonNode, JsonPointer, JsonNode>> jsonNodes = new ArrayList<>(jsonNodeStack);
      jsonNodeStack.clear();
      jsonNodes.stream().forEach(t -> Seq.seq(t.v3.fieldNames())
          .forEach(field -> {
            JsonPointer propertyPointer = t.v2.append(field);
            JsonNode propertyDefaultValue = t.v3.get(field);
            if (propertyDefaultValue.isObject() && t.v1.has(field)) {
              jsonNodeStack.add(Tuple.tuple(
                  t.v1.get(field), propertyPointer, propertyDefaultValue));
            } else if (!t.v1.has(field)) {
              operations.add(new AddOperation(propertyPointer, propertyDefaultValue));
            }
          }));
    }

    return operations;
  }

  default JsonPatchOperation applyReplaceValue(JsonPointer basePointer, JsonNode valueNode) {
    return new ReplaceOperation(basePointer, valueNode);
  }

  default JsonPatchOperation applyAddValue(JsonPointer basePointer, JsonNode valueNode) {
    return new AddOperation(basePointer, valueNode);
  }

  default JsonPatchOperation applyMoveValue(JsonPointer from, JsonPointer path) {
    return new MoveOperation(from, path);
  }

  default JsonPatchOperation applyRemoveValue(JsonPointer path) {
    return new RemoveOperation(path);
  }

}
