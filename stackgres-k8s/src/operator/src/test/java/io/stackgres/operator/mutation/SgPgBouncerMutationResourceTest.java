/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgPgBouncerMutationResourceTest
    extends MutationResourceTest<StackGresPoolingConfig, PoolingReview> {

  @Override
  protected MutationResource<StackGresPoolingConfig, PoolingReview> getResource() {
    return new SgPgBouncerMutationResource(pipeline);
  }

  @Override
  protected PoolingReview getReview() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

}
