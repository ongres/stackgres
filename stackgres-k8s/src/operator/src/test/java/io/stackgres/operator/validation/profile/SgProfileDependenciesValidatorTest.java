/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class SgProfileDependenciesValidatorTest
    extends DependenciesValidatorTest<SgProfileReview, SgProfileDependenciesValidator> {

  @Override
  protected SgProfileDependenciesValidator setUpValidation() {
    return new SgProfileDependenciesValidator();
  }

  @Override
  protected SgProfileReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return AdmissionReviewFixtures.instanceProfile().loadCreate().get();
  }

  @Override
  protected SgProfileReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return AdmissionReviewFixtures.instanceProfile().loadUpdate().get();
  }

  @Override
  protected SgProfileReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return AdmissionReviewFixtures.instanceProfile().loadDelete().get();
  }

  @Override
  protected SgProfileReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt()
      throws ValidationFailed {
    return AdmissionReviewFixtures.instanceProfile().loadDelete().get();
  }

  @Override
  protected SgProfileReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return AdmissionReviewFixtures.instanceProfile().loadDelete().get();
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().setSgInstanceProfile(null);
  }
}
