/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public abstract class DefaultValuesMutator<R extends CustomResource<?, ?>,
    T extends AdmissionReview<R>>
    implements JsonPatchMutator<T> {

  private ObjectMapper jsonMapper;

  private DefaultCustomResourceFactory<R> factory;

  private JsonNode defaultNode;

  @PostConstruct
  public void init() {
    R defaultResource = factory.buildResource();
    defaultNode = getTargetNode(defaultResource);
  }

  @Inject
  public void setFactory(DefaultCustomResourceFactory<R> factory) {
    this.factory = factory;
  }

  @Inject
  public void setObjectMapper(ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  public JsonNode getTargetNode(R resource) {
    return jsonMapper.valueToTree(resource);
  }

  protected List<JsonPatchOperation> mutate(JsonPointer basePointer,
                                            R incomingResource) {
    JsonNode incomingNode = getTargetNode(incomingResource);
    return applyDefaults(basePointer, defaultNode, incomingNode);
  }

}
