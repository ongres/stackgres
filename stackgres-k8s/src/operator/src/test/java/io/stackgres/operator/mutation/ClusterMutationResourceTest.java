/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class ClusterMutationResourceTest
    extends MutationResourceTest<StackGresCluster, StackGresClusterReview> {

  @Override
  protected MutationResource<StackGresCluster, StackGresClusterReview> getResource() {
    return new ClusterMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresClusterReview getReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }
}
