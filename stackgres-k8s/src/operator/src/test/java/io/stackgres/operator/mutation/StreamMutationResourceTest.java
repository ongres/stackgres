/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamMutationResourceTest extends MutationResourceTest<StackGresStream, StackGresStreamReview> {

  @Override
  protected AbstractMutationResource<StackGresStream, StackGresStreamReview> getResource() {
    return new StreamMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresStreamReview getReview() {
    return AdmissionReviewFixtures.stream().loadCreate().get();
  }

}
