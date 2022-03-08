/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgConfigDependenciesValidatorTest
    extends DependenciesValidatorTest<PgConfigReview, PgConfigDependenciesValidator> {

  @Override
  protected DependenciesValidator<PgConfigReview> setUpValidation() {
    return new PgConfigDependenciesValidator();
  }

  @Override
  protected PgConfigReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  protected PgConfigReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return AdmissionReviewFixtures.postgresConfig().loadUpdate().get();
  }

  @Override
  protected PgConfigReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

  @Override
  protected PgConfigReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt()
      throws ValidationFailed {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

  @Override
  protected PgConfigReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().getConfiguration().setPostgresConfig(null);
  }
}
