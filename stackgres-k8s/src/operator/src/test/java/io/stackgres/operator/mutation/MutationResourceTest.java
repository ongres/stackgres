/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class MutationResourceTest<R extends HasMetadata, T extends AdmissionReview<R>> {

  @Mock
  protected MutationPipeline<R, T> pipeline;

  protected AbstractMutationResource<R, T> resource;

  protected T review;

  @BeforeEach
  void setUp() {
    resource = getResource();

    review = getReview();
  }

  protected abstract AbstractMutationResource<R, T> getResource();

  protected abstract T getReview();

  @SuppressWarnings("unchecked")
  @Test
  void givenAnValidAdmissionReview_itShouldReturnAnyPath() {
    doAnswer(invocation -> {
      return ((R) invocation.getArgument(1));
    }).when(pipeline).mutate(any(), any());

    AdmissionReviewResponse response = resource.mutate(review);

    assertNull(response.getResponse().getPatchType());
    assertNull(response.getResponse().getPatch());
  }

  @SuppressWarnings("unchecked")
  @Test
  void givenAnInvalidAdmissionReview_itShouldReturnABase64EncodedPath() {
    doAnswer(invocation -> {
      ((R) invocation.getArgument(1)).getMetadata().setAnnotations(
          Seq.seq(Optional.ofNullable(
              ((R) invocation.getArgument(1)).getMetadata().getAnnotations())
              .orElse(Map.of()))
          .append(Seq.seq(Map.of("fake", "annotation")))
          .toMap(Tuple2::v1, Tuple2::v2));
      return ((R) invocation.getArgument(1));
    }).when(pipeline).mutate(any(), any());

    AdmissionReviewResponse response = resource.mutate(review);

    assertEquals(
        "W3sib3AiOiJhZGQiLCJwYXRoIjoiL21ldGFkYXRhL2Fubm90"
        + "YXRpb25zL2Zha2UiLCJ2YWx1ZSI6ImFubm90YXRpb24ifV0=",
        response.getResponse().getPatch());
    assertEquals("JSONPatch", response.getResponse().getPatchType());
  }

}
