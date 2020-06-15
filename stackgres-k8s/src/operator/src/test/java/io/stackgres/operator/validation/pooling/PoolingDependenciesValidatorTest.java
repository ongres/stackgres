/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import io.stackgres.testutil.JsonUtil;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operator.common.PoolingReview;

import org.junit.jupiter.api.Test;
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
    @Test
    protected void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {

        PoolingReview review = JsonUtil.readFromJson("pooling_allow_request/create.json",
                PoolingReview.class);

        givenAReviewCreation_itShouldDoNothing(review);

    }

    @Override
    @Test
    protected void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

        PoolingReview review = JsonUtil.readFromJson("pooling_allow_request/update.json",
                PoolingReview.class);

        givenAReviewUpdate_itShouldDoNothing(review);

    }

    @Override
    @Test
    protected void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt() {

        PoolingReview review = JsonUtil.readFromJson("pooling_allow_request/delete.json",
                PoolingReview.class);

        givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt(review);

    }

    @Override
    @Test
    protected void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt() throws ValidationFailed {

        PoolingReview review = JsonUtil.readFromJson("pooling_allow_request/delete.json",
                PoolingReview.class);

        givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt(review);

    }
}
