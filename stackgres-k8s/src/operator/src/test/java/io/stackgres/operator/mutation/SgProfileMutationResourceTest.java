/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SgProfileMutationResourceTest
    extends MutationResourceTest<StackGresProfile, SgProfileReview> {

  @Override
  protected MutationResource<StackGresProfile, SgProfileReview> getResource() {
    return new SgProfileMutationResource(pipeline);
  }

  @Override
  protected SgProfileReview getReview() {
    return AdmissionReviewFixtures.instanceProfile().loadCreate().get();
  }
}
