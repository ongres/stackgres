/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.Streams;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public abstract class DefaultValuesMutatorTest<R extends CustomResource, T extends AdmissionReview<R>> {

  protected static final ObjectMapper mapper = new ObjectMapper();

  protected DefaultValuesMutator<R, T> mutator;

  @Mock
  protected DefaultCustomResourceFactory<R> factory;

  @BeforeEach
  void setUp() {
    when(factory.buildResource()).thenReturn(getDefaultResource());
    mutator = getMutatorInstance();
    mutator.setFactory(factory);
    mutator.init();
  }

  protected abstract DefaultValuesMutator<R, T> getMutatorInstance();

  protected abstract T getEmptyReview();

  protected abstract T getDefaultReview();

  protected abstract R getDefaultResource();

  @Test
  public void givenAnEmptyConf_itShouldReturnAPatchForEveryDefaultsProperty() {

    T review = getEmptyReview();

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode targetNode = mutator.getTargetNode(getDefaultReview().getRequest().getObject());

    assertEquals(Streams.stream(targetNode.fieldNames()).count(), operations.size());

  }

  @Test
  public void givenAConfWithAllDefaultsValuesSettled_itShouldNotReturnAnyPatch() {

    T review = getDefaultReview();

    List<JsonPatchOperation> operators = mutator.mutate(review);

    assertEquals(0, operators.size());

  }

  @Test
  public void returnedOperationsMustBeValidJsonPatches() throws JsonPatchException {

    T review = getEmptyReview();

    JsonNode crJson = mapper.valueToTree(review.getRequest().getObject());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    JsonNode targetNode = getConfJson(newConfig);

    JsonNode defaultTarget = mutator.getTargetNode(getDefaultReview().getRequest().getObject());

    assertEquals(targetNode, defaultTarget);

  }

  protected abstract JsonNode getConfJson(JsonNode crJson);


}
