/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
class ShardedClusterValidationPipelineTest
    extends ValidationPipelineTest<StackGresShardedCluster, StackGresShardedClusterReview> {

  @Inject
  public ShardedClusterValidationPipeline pipeline;

  @Override
  public StackGresShardedClusterReview getConstraintViolatingReview() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures
        .shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getCoordinator().setInstances(0);
    return review;
  }

  @Override
  public ValidationPipeline<StackGresShardedClusterReview> getPipeline() {
    return pipeline;
  }

}
