/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.DependenciesValidatorTest;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgBouncerDependenciesValidatorTest
        extends DependenciesValidatorTest<PgBouncerReview, PgBouncerDependenciesValidator> {

    @BeforeEach
    void setUp() {
        validator = new PgBouncerDependenciesValidator(clusterScanner);
    }

    @Override
    @Test
    protected void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {

        PgBouncerReview review = JsonUtil.readFromJson("pgbouncer_allow_request/create.json",
                PgBouncerReview.class);

        givenAReviewCreation_itShouldDoNothing(review);

    }

    @Override
    @Test
    protected void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

        PgBouncerReview review = JsonUtil.readFromJson("pgbouncer_allow_request/update.json",
                PgBouncerReview.class);

        givenAReviewUpdate_itShouldDoNothing(review);

    }

    @Override
    @Test
    protected void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt() {

        PgBouncerReview review = JsonUtil.readFromJson("pgbouncer_allow_request/delete.json",
                PgBouncerReview.class);

        givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt(review);

    }

    @Override
    @Test
    protected void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt() throws ValidationFailed {

        PgBouncerReview review = JsonUtil.readFromJson("pgbouncer_allow_request/delete.json",
                PgBouncerReview.class);

        givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt(review);

    }
}
