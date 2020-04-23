/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;
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
    @Test
    protected void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {
        SgProfileReview review = JsonUtil.readFromJson("sgprofile_allow_request/create.json",
                SgProfileReview.class);

        givenAReviewCreation_itShouldDoNothing(review);
    }

    @Override
    @Test
    protected void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

        SgProfileReview review = JsonUtil.readFromJson("sgprofile_allow_request/update.json",
                SgProfileReview.class);

        givenAReviewUpdate_itShouldDoNothing(review);

    }

    @Override
    @Test
    protected void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt() {

        SgProfileReview review = JsonUtil.readFromJson("sgprofile_allow_request/delete.json",
                SgProfileReview.class);

        givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt(review);

    }

    @Override
    @Test
    protected void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt() throws ValidationFailed {

        SgProfileReview review = JsonUtil.readFromJson("sgprofile_allow_request/delete.json",
                SgProfileReview.class);

        givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt(review);

    }
}
