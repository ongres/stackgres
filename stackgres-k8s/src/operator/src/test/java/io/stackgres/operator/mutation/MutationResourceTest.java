/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class MutationResourceTest<T extends AdmissionReview<?>> {

  @Mock
  protected JsonPatchMutationPipeline<T> pipeline;

  protected MutationResource<T> resource;

  protected T review;

  @Test
  void givenAnValidAdmissionReview_itShouldReturnAnyPath() {

    when(pipeline.mutate(review)).thenReturn(Optional.empty());

    AdmissionReviewResponse response = resource.mutate(review);

    assertNull(response.getResponse().getPatchType());
    assertNull(response.getResponse().getPatch());

  }

  @Test
  void givenAnInvalidAdmissionReview_itShouldReturnABase64EncodedPath() {
    String fakePatch = "fake patch";

    when(pipeline.mutate(review)).thenReturn(Optional.of(fakePatch));

    AdmissionReviewResponse response = resource.mutate(review);

    assertEquals("ZmFrZSBwYXRjaA==", response.getResponse().getPatch());
    assertEquals("JSONPatch", response.getResponse().getPatchType());

  }

}
