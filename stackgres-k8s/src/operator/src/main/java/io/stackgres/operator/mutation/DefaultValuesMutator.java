/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.AdmissionReview;
import io.stackgres.operatorframework.JsonPatchMutator;

public abstract class DefaultValuesMutator<R extends CustomResource, T extends AdmissionReview<R>>
    implements JsonPatchMutator<T> {

  protected static final ObjectMapper mapper = new ObjectMapper();

  private DefaultCustomResourceFactory<R> factory;

  private JsonNode defaultNode;

  @PostConstruct
  public void init() {
    R defaultResource = factory.buildResource();
    defaultNode = getTargetNode(defaultResource);
  }

  public JsonNode getTargetNode(R resource) {
    JsonNode resourceNode = mapper.valueToTree(resource);
    return resourceNode.get("spec");
  }

  protected List<JsonPatchOperation> mutate(JsonPointer basePointer,
                                            R incomingResource) {

    List<JsonPatchOperation> operations = new ArrayList<>();

    JsonNode incomingNode = getTargetNode(incomingResource);

    defaultNode.fieldNames().forEachRemaining(field -> {
      if (!incomingNode.has(field)) {
        JsonPointer propertyPointer = basePointer.append(field);
        operations.add(new AddOperation(propertyPointer, defaultNode.get(field)));
      }
    });

    return operations;
  }

  @Inject
  public void setFactory(DefaultCustomResourceFactory<R> factory) {
    this.factory = factory;
  }
}
