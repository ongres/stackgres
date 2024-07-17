/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgPgBouncerMutationResourceTest
    extends MutationResourceTest<StackGresPoolingConfig, StackGresPoolingConfigReview> {

  @Override
  protected AbstractMutationResource<StackGresPoolingConfig, StackGresPoolingConfigReview> getResource() {
    return new SgPgBouncerMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresPoolingConfigReview getReview() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

}
