/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgConfigDependenciesValidatorTest
    extends DependenciesValidatorTest<StackGresPostgresConfigReview, PgConfigDependenciesValidator> {

  @Override
  protected PgConfigDependenciesValidator setUpValidation() {
    return new PgConfigDependenciesValidator();
  }

  @Override
  protected StackGresPostgresConfigReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  protected StackGresPostgresConfigReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return AdmissionReviewFixtures.postgresConfig().loadUpdate().get();
  }

  @Override
  protected StackGresPostgresConfigReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

  @Override
  protected StackGresPostgresConfigReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt()
      throws ValidationFailed {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

  @Override
  protected StackGresPostgresConfigReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

  @Override
  protected void makeClusterDependant(StackGresCluster cluster, StackGresPostgresConfigReview review) {
    cluster.getSpec().getConfigurations().setSgPostgresConfig(review.getRequest().getName());
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().getConfigurations().setSgPostgresConfig(null);
  }
}
