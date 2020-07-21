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
import com.fasterxml.jackson.databind.node.ArrayNode;
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

public abstract class DefaultStateMutator<R extends CustomResource, T extends AdmissionReview<R>>
    implements JsonPatchMutator<T> {

  protected static final ObjectMapper mapper = new ObjectMapper();

  private DefaultCustomResourceFactory<R> factory;

  private R defaultResource;

  @PostConstruct
  public void init() {
    defaultResource = factory.buildResource();
  }

  public JsonNode getTargetNode(R resource) {
    JsonNode resourceNode = mapper.valueToTree(resource);
    return resourceNode;
  }

  protected List<JsonPatchOperation> mutate(JsonPointer basePointer,
      R incomingResource) {

    Map<String, String> parameters = getParametersNode(incomingResource);
    Map<String, String> defaultParameters = getParametersNode(defaultResource);
    ArrayNode defaultsListNode = mapper.createArrayNode()
        .addAll(Seq.seq(defaultParameters)
            .filter(defaultParameter -> Seq.seq(parameters)
                .noneMatch(parameter -> parameter.v1.equals(defaultParameter.v1)
                    && !parameter.v2.equals(defaultParameter.v2)))
            .map(Tuple2::v1)
            .map(TextNode::new)
            .toList());

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
