/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.HashMap;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class PgConfigValidationPipelineTest
    extends ValidationPipelineTest<StackGresPostgresConfig, StackGresPostgresConfigReview> {

  @Inject
  public PgConfigValidationPipeline pipeline;

  @Override
  public StackGresPostgresConfigReview getConstraintViolatingReview() {
    StackGresPostgresConfigReview review = getValidReview();

    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());

    return review;
  }

  private StackGresPostgresConfigReview getValidReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  public ValidationPipeline<StackGresPostgresConfigReview> getPipeline() {
    return pipeline;
  }
}
