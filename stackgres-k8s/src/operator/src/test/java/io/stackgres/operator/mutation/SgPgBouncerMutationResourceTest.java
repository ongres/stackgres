/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.testutil.JsonUtil;
import io.stackgres.operator.common.PoolingReview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgPgBouncerMutationResourceTest extends MutationResourceTest<PoolingReview> {

  @BeforeEach
  void setUp() {
    final SgPgBouncerMutationResource resource = new SgPgBouncerMutationResource();
    resource.setPipeline(pipeline);
    this.resource = resource;

    review = JsonUtil
        .readFromJson("pooling_allow_request/create.json", PoolingReview.class);
  }

  @Override
  @Test
  void givenAnValidAdmissionReview_itShouldReturnAnyPath() {
    super.givenAnValidAdmissionReview_itShouldReturnAnyPath();
  }

  @Override
  @Test
  void givenAnInvalidAdmissionReview_itShouldReturnABase64EncodedPath() {
    super.givenAnInvalidAdmissionReview_itShouldReturnABase64EncodedPath();
  }
}