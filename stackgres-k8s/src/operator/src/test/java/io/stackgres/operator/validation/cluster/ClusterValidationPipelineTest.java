/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
class ClusterValidationPipelineTest
    extends ValidationPipelineTest<StackGresCluster, StackGresClusterReview> {

  @Inject
  public ClusterValidationPipeline pipeline;

  @Override
  public StackGresClusterReview getConstraintViolatingReview() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().setInstances(0);
    return review;
  }

  @Override
  public ValidationPipeline<StackGresClusterReview> getPipeline() {
    return pipeline;
  }

}
