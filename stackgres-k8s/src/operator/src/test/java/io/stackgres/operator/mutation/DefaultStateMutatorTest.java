/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class DefaultStateMutatorTest
      <R extends CustomResource<?, ?>, T extends AdmissionReview<R>> {

  protected AbstractStateMutator<R, T> mutator;

  @Mock
  protected DefaultCustomResourceFactory<R> factory;

  @BeforeEach
  void setUp() {
    when(factory.buildResource()).thenReturn(getDefaultResource());
    mutator = getMutatorInstance(factory);
    mutator.init();
  }

  protected abstract AbstractStateMutator<R, T> getMutatorInstance(
      DefaultCustomResourceFactory<R> factory);

  protected abstract T getEmptyReview();

  protected abstract T getDefaultReview();

  protected abstract R getDefaultResource();

  @Test
  void givenAnEmptyConf_itShouldReturnAModfiedObjectWithDefaultStatus() {
    T review = getEmptyReview();

    R result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    Assertions.assertNotEquals(review.getRequest().getObject(), result);
    Assertions.assertEquals(
        getDefaultResource().getStatus(), result.getStatus());
  }

  @Test
  public void givenConfDefaultsValues_shouldReturnAModfiedObjectWithDefaultStatus() {
    T review = getDefaultReview();

    R result = mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));

    Assertions.assertNotEquals(review.getRequest().getObject(), result);
    Assertions.assertEquals(
        getDefaultResource().getStatus(), result.getStatus());
  }

}
