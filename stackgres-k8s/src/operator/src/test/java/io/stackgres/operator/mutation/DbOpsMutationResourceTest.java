/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsMutationResourceTest extends MutationResourceTest<StackGresDbOps, DbOpsReview> {

  @Override
  protected MutationResource<StackGresDbOps, DbOpsReview> getResource() {
    return new DbOpsMutationResource(pipeline);
  }

  @Override
  protected DbOpsReview getReview() {
    return AdmissionReviewFixtures.dbOps().loadRestartCreate().get();
  }

}
