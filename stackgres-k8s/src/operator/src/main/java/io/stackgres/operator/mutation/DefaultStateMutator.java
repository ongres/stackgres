/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public abstract class DefaultStateMutator
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    implements JsonPatchMutator<T> {

  private ObjectMapper objectMapper;

  private DefaultCustomResourceFactory<R> factory;

  private R defaultResource;

  @PostConstruct
  public void init() {
    this.defaultResource = factory.buildResource();
  }

  @Inject
  public void setFactory(DefaultCustomResourceFactory<R> factory) {
    this.factory = factory;
  }

  @Inject
  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public JsonNode getTargetNode(R resource) {
    return objectMapper.valueToTree(resource);
  }

  protected List<JsonPatchOperation> mutate(JsonPointer basePointer,
      R incomingResource) {
    Map<String, String> defaultParameters = getParametersNode(defaultResource);
    ObjectNode defaultsListNode = objectMapper.createObjectNode()
        .setAll(Seq.seq(defaultParameters)
            .map(t -> t.map2(TextNode::new))
            .toMap(Tuple2::v1, Tuple2::v2));

    JsonNode incomingNode = getTargetNode(incomingResource);

    if (incomingNode == null) {
      return List.of(buildAddOperation(basePointer, defaultsListNode));
    } else {
      return List.of(buildReplaceOperation(basePointer, defaultsListNode));
    }
  }

  protected abstract Map<String, String> getParametersNode(R incomingResource);

}
