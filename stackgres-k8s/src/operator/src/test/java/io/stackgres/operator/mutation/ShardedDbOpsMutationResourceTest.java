/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsMutationResourceTest
    extends MutationResourceTest<StackGresShardedDbOps, ShardedDbOpsReview> {

  @Override
  protected AbstractMutationResource<StackGresShardedDbOps, ShardedDbOpsReview> getResource() {
    return new ShardedDbOpsMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected ShardedDbOpsReview getReview() {
    return AdmissionReviewFixtures.shardedDbOps().loadRestartCreate().get();
  }

}
