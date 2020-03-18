/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operator.common.PgConfigReview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgConfigDependenciesValidatorTest extends DependenciesValidatorTest<PgConfigReview, PgConfigDependenciesValidator> {

    @Override
    protected DependenciesValidator<PgConfigReview> setUpValidation() {
        return new PgConfigDependenciesValidator();
    }

    @Override
    @Test
    public void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {

        PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig.json",
                PgConfigReview.class);

        givenAReviewCreation_itShouldDoNothing(review);

    }

    @Override
    @Test
    public void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

        PgConfigReview review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
                PgConfigReview.class);

        givenAReviewUpdate_itShouldDoNothing(review);

    }

    @Override
    @Test
    public void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt() {

        PgConfigReview review = JsonUtil
                .readFromJson("pgconfig_allow_request/pgconfig_delete.json",
                        PgConfigReview.class);

        givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt(review);

    }

    @Override
    @Test
    public void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt() throws ValidationFailed {

        PgConfigReview review = JsonUtil
                .readFromJson("pgconfig_allow_request/pgconfig_delete.json",
                        PgConfigReview.class);

        givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt(review);

    }
}
