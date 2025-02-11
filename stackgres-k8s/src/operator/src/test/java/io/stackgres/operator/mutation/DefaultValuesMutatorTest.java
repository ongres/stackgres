/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class DefaultValuesMutatorTest
      <R extends CustomResource<?, ?>, T extends AdmissionReview<R>, S extends HasMetadata> {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected AbstractValuesMutator<R, T, S> mutator;

  protected DefaultCustomResourceFactory<R, S> factory;

  @BeforeEach
  void setUp() {
    factory = createFactory();
    mutator = getMutatorInstance(factory, JSON_MAPPER);
  }

  protected abstract DefaultCustomResourceFactory<R, S> createFactory();

  protected abstract AbstractValuesMutator<R, T, S> getMutatorInstance(
      DefaultCustomResourceFactory<R, S> factory,
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
    review.getRequest().setObject(getDefaultResource());

    JsonNode expected = JsonUtil.toJson(review.getRequest().getObject().getSpec());

    R result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expected,
        JsonUtil.toJson(result.getSpec()));
  }

}
