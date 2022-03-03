/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;

@ApplicationScoped
public class ObjectStorageDefaultValuesMutator
    extends DefaultValuesMutator<StackGresObjectStorage, ObjectStorageReview>
    implements ObjectStorageMutator {

  @Override
  public JsonNode getTargetNode(StackGresObjectStorage resource) {
    return super.getTargetNode(resource)
        .get("spec");
  }

  @Override
  public List<JsonPatchOperation> mutate(ObjectStorageReview review) {
    return mutate(SG_OBJECT_STORAGE_POINTER, review.getRequest().getObject());
  }

  @Override
  public List<JsonPatchOperation> applyDefaults(JsonPointer basePointer,
                                                JsonNode defaultNode,
                                                JsonNode incomingNode) {
    if (incomingNode.has("type")
        && !incomingNode.get("type").equals(
        defaultNode.get("type"))) {
      defaultNode = FACTORY.objectNode();
    }
    return super.applyDefaults(basePointer, defaultNode, incomingNode);
  }
}
