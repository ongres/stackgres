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
import com.github.fge.jsonpatch.ReplaceOperation;

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

  default List<JsonPatchOperation> applyDefaults(JsonPointer basePointer,
                                                 JsonNode defaultNode,
                                                 JsonNode incomingNode) {

    List<JsonPatchOperation> operations = new ArrayList<>();

    defaultNode.fieldNames().forEachRemaining(field -> {
      if (!incomingNode.has(field)) {
        JsonPointer propertyPointer = basePointer.append(field);
        operations.add(new AddOperation(propertyPointer, defaultNode.get(field)));
      }
    });

    return operations;
  }

}
