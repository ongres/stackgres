/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.MoveOperation;
import com.github.fge.jsonpatch.RemoveOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;

public interface JsonPatchMutator<T> {

  JsonNodeFactory FACTORY = JacksonUtils.nodeFactory();

  List<JsonPatchOperation> mutate(T review);

  default boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }

  default JsonPatchOperation buildAddOperation(JsonPointer path, String value) {
    return buildAddOperation(path, FACTORY.textNode(value));
  }

  default JsonPatchOperation buildAddOperation(JsonPointer path, JsonNode value) {
    return new AddOperation(path, value);
  }

  default JsonPatchOperation buildReplaceOperation(JsonPointer path, String value) {
    return buildReplaceOperation(path, FACTORY.textNode(value));
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
