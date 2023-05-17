/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class DefaultValuesMutatorTest
      <R extends CustomResource<?, ?>, T extends AdmissionReview<R>> {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected AbstractValuesMutator<R, T> mutator;

  @Mock
  protected DefaultCustomResourceFactory<R> factory;

  @BeforeEach
  void setUp() {
    when(factory.buildResource()).thenReturn(getDefaultResource());
    mutator = getMutatorInstance(factory, JSON_MAPPER);
    mutator.init();
  }

  protected abstract AbstractValuesMutator<R, T> getMutatorInstance(
      DefaultCustomResourceFactory<R> factory,
      JsonMapper jsonMapper);

  protected abstract T getEmptyReview();

  protected abstract T getDefaultReview();

  protected abstract R getDefaultResource();

  @Test
  void givenAnEmptyConf_itShouldReturnAModfiedObjectWithDefaultSpec() {
    T review = getEmptyReview();

    JsonNode expected = JsonUtil.toJson(getDefaultResource().getSpec());

    R result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    Assertions.assertNotEquals(review.getRequest().getObject(), result);
    JsonUtil.assertJsonEquals(
        expected,
        JsonUtil.toJson(result.getSpec()));
  }

  @Test
  void givenAConfWithAllDefaultsValuesSettled_itShouldReturnTheSameObject() {
    T review = getDefaultReview();

    JsonNode expected = JsonUtil.toJson(review.getRequest().getObject().getSpec());

    R result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expected,
        JsonUtil.toJson(result.getSpec()));
  }

}
