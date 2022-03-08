/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.PoolingReview;
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
class PoolingDependenciesValidatorTest
    extends DependenciesValidatorTest<PoolingReview, PoolingDependenciesValidator> {

  @Override
  protected DependenciesValidator<PoolingReview> setUpValidation() {
    return new PoolingDependenciesValidator();
  }

  @Override
  protected PoolingReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

  @Override
  protected PoolingReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return AdmissionReviewFixtures.poolingConfig().loadUpdate().get();
  }

  @Override
  protected PoolingReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return AdmissionReviewFixtures.poolingConfig().loadDelete().get();
  }

  @Override
  protected PoolingReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt()
      throws ValidationFailed {
    return AdmissionReviewFixtures.poolingConfig().loadDelete().get();
  }

  @Override
  protected PoolingReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return AdmissionReviewFixtures.poolingConfig().loadDelete().get();
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().getConfiguration().setConnectionPoolingConfig(null);
  }
}
