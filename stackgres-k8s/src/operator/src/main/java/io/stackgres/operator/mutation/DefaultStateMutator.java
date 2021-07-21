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
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public abstract class DefaultStateMutator
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    implements JsonPatchMutator<T> {

  protected static final ObjectMapper MAPPER = new ObjectMapper();

  private DefaultCustomResourceFactory<R> factory;

  private R defaultResource;

  @PostConstruct
  public void init() {
    defaultResource = factory.buildResource();
  }

  public JsonNode getTargetNode(R resource) {
    JsonNode resourceNode = MAPPER.valueToTree(resource);
    return resourceNode;
  }

  protected List<JsonPatchOperation> mutate(JsonPointer basePointer,
      R incomingResource) {
    Map<String, String> defaultParameters = getParametersNode(defaultResource);
    ObjectNode defaultsListNode = MAPPER.createObjectNode()
        .setAll(Seq.seq(defaultParameters)
            .map(t -> t.map2(TextNode::new))
            .toMap(Tuple2::v1, Tuple2::v2));

    JsonNode incomingNode = getTargetNode(incomingResource);

    if (incomingNode == null) {
      return ImmutableList.of(buildAddOperation(basePointer, defaultsListNode));
    } else {
      return ImmutableList.of(buildReplaceOperation(basePointer, defaultsListNode));
    }
  }

  protected abstract Map<String, String> getParametersNode(R incomingResource);

  @Inject
  public void setFactory(DefaultCustomResourceFactory<R> factory) {
    this.factory = factory;
  }
}
