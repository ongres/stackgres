/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;
import org.junit.jupiter.api.Disabled;

@QuarkusTest
@Disabled
class ClusterValidationPipelineIt
    extends ValidationPipelineTest<StackGresCluster, StackGresClusterReview> {

  @Override
  public StackGresClusterReview getConstraintViolatingReview() {
    final StackGresClusterReview review = JsonUtil.readFromJson("cluster_allow_requests/valid_creation.json",
        StackGresClusterReview.class);
    review.getRequest().getObject().getSpec().setInstances(0);
    return review;
  }


}