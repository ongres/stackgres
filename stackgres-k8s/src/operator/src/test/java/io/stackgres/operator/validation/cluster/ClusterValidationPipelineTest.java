/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operator.validation.backup.BackupValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@QuarkusTest
@DisabledIfEnvironmentVariable(named = "SKIP_QUARKUS_TEST", matches = "true")
class ClusterValidationPipelineTest
    extends ValidationPipelineTest<StackGresCluster, StackGresClusterReview> {

  @Inject
  public ClusterValidationPipeline pipeline;

  @Override
  public StackGresClusterReview getConstraintViolatingReview() {
    final StackGresClusterReview review = JsonUtil.readFromJson("cluster_allow_requests/valid_creation.json",
        StackGresClusterReview.class);
    review.getRequest().getObject().getSpec().setInstances(0);
    return review;
  }

  @Override
  public ValidationPipeline<StackGresClusterReview> getPipeline() {
    return pipeline;
  }

}