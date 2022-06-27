/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class ClusterMutationResourceTest extends MutationResourceTest<StackGresClusterReview> {

  @BeforeEach
  void setUp() {
    final ClusterMutationResource resource = new ClusterMutationResource();
    resource.setPipeline(pipeline);
    this.resource = resource;

    review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackGresClusterReview.class);
  }
}
