/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsMutationResourceTest extends MutationResourceTest<StackGresDbOps, StackGresDbOpsReview> {

  @Override
  protected AbstractMutationResource<StackGresDbOps, StackGresDbOpsReview> getResource() {
    return new DbOpsMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresDbOpsReview getReview() {
    return AdmissionReviewFixtures.dbOps().loadRestartCreate().get();
  }

}
