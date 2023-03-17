/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterMutationResourceTest
    extends MutationResourceTest<StackGresShardedCluster, StackGresShardedClusterReview> {

  @Override
  protected MutationResource<StackGresShardedCluster, StackGresShardedClusterReview> getResource() {
    return new ShardedClusterMutationResource(pipeline);
  }

  @Override
  protected StackGresShardedClusterReview getReview() {
    return AdmissionReviewFixtures.shardedCluster().loadCreate().get();
  }
}
