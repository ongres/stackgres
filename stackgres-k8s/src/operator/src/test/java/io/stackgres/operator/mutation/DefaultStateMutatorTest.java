/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class DefaultStateMutatorTest<R extends CustomResource<?, ?>, T extends AdmissionReview<R>> {

  protected static final ObjectMapper mapper = new ObjectMapper();

  protected DefaultStateMutator<R, T> mutator;

  @Mock
  protected DefaultCustomResourceFactory<R> factory;

  @BeforeEach
  void setUp() {
    when(factory.buildResource()).thenReturn(getDefaultResource());
    mutator = getMutatorInstance();
    mutator.setFactory(factory);
    mutator.init();
  }

  protected abstract DefaultStateMutator<R, T> getMutatorInstance();

  protected abstract T getEmptyReview();

  protected abstract T getDefaultReview();

  protected abstract R getDefaultResource();

  @Test
  public void givenAnEmptyConf_itShouldReturnAPatchForEveryMissingParentAndOneForDefaultsProperty() {

    T review = getEmptyReview();

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(getMissingParentsCount() + 1, operations.size());

  }

  @Test
  public void givenAConfWithAllDefaultsValuesSettled_itShouldReturnAPatchForEveryMissingParentAndOneForDefaultsProperty() {

    T review = getDefaultReview();

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertEquals(getMissingParentsCount() + 1, operations.size());

  }

  @Test
  public void returnedOperationsMustBeValidJsonPatches() throws JsonPatchException {

    T review = getEmptyReview();

    JsonNode crJson = mapper.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    JsonNode targetNode = getConfJson(newConfig);

    JsonNode defaultTarget = mapper.createObjectNode()
        .setAll(Seq.seq(getConfigParameters(factory.buildResource()))
            .map(t -> t.map2(TextNode::new)).toMap(Tuple2::v1, Tuple2::v2));

    assertEquals(defaultTarget, targetNode);

  }

  protected abstract int getMissingParentsCount();

  protected abstract Map<String, String> getConfigParameters(R resource);

  protected abstract JsonNode getConfJson(JsonNode crJson);

}
