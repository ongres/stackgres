/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgProfileMutationResourceTest
    extends MutationResourceTest<StackGresProfile, StackGresInstanceProfileReview> {

  @Override
  protected AbstractMutationResource<StackGresProfile, StackGresInstanceProfileReview> getResource() {
    return new SgProfileMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresInstanceProfileReview getReview() {
    return AdmissionReviewFixtures.instanceProfile().loadCreate().get();
  }
}
