/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public abstract class DefaultValuesMutator<R extends CustomResource<?, ?>,
    T extends AdmissionReview<R>>
    implements JsonPatchMutator<T> {

  private final ObjectMapper jsonMapper;

  private final JsonNode defaultNode;

  protected DefaultValuesMutator(DefaultCustomResourceFactory<R> factory,
      ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
    this.defaultNode = getSourceNode(factory.buildResource());
  }

  public DefaultValuesMutator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.jsonMapper = null;
    this.defaultNode = null;
  }

  protected abstract JsonNode getSourceNode(R resource);

  protected abstract JsonNode getTargetNode(R resource);

  protected JsonNode toNode(R resource) {
    return jsonMapper.valueToTree(resource);
  }

  protected List<JsonPatchOperation> mutate(
      JsonPointer basePointer,
      R incomingResource) {
    JsonNode incomingNode = getTargetNode(incomingResource);
    return applyDefaults(basePointer, defaultNode, incomingNode);
  }

}
