/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class SgProfileDependenciesValidatorTest
        extends DependenciesValidatorTest<SgProfileReview, SgProfileDependenciesValidator> {

    @Override
    protected DependenciesValidator<SgProfileReview> setUpValidation() {
        return new SgProfileDependenciesValidator();
    }

    @Override
    protected SgProfileReview getReview_givenAReviewCreation_itShouldDoNothing() {
      return JsonUtil.readFromJson("sgprofile_allow_request/create.json",
          SgProfileReview.class);
    }

    @Override
    protected SgProfileReview getReview_givenAReviewUpdate_itShouldDoNothing() {
      return JsonUtil.readFromJson("sgprofile_allow_request/update.json",
          SgProfileReview.class);
    }

    @Override
    protected SgProfileReview getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
      return JsonUtil.readFromJson("sgprofile_allow_request/delete.json",
          SgProfileReview.class);
    }

    @Override
    protected SgProfileReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt()
        throws ValidationFailed {
      return JsonUtil.readFromJson("sgprofile_allow_request/delete.json",
          SgProfileReview.class);
    }

    @Override
    protected SgProfileReview getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
      return JsonUtil.readFromJson("sgprofile_allow_request/delete.json",
          SgProfileReview.class);
    }

    @Override
    protected void makeClusterNotDependant(StackGresCluster cluster) {
      cluster.getSpec().setResourceProfile(null);
    }
}
