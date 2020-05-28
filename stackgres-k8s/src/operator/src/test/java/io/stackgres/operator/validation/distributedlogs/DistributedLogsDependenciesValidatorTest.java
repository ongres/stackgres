/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import io.stackgres.testutil.JsonUtil;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
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
class DistributedLogsDependenciesValidatorTest
        extends DependenciesValidatorTest<StackGresDistributedLogsReview, DistributedLogsDependenciesValidator> {

    @Override
    protected DependenciesValidator<StackGresDistributedLogsReview> setUpValidation() {
        return new DistributedLogsDependenciesValidator();
    }

    @Override
    @Test
    protected void givenAReviewCreation_itShouldDoNothing() throws ValidationFailed {
      StackGresDistributedLogsReview review = JsonUtil.readFromJson("distributedlogs_allow_request/create.json",
          StackGresDistributedLogsReview.class);

        givenAReviewCreation_itShouldDoNothing(review);
    }

    @Override
    @Test
    protected void givenAReviewUpdate_itShouldDoNothing() throws ValidationFailed {

      StackGresDistributedLogsReview review = JsonUtil.readFromJson("distributedlogs_allow_request/update.json",
          StackGresDistributedLogsReview.class);

        givenAReviewUpdate_itShouldDoNothing(review);

    }

    @Override
    @Test
    protected void givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt() {

      StackGresDistributedLogsReview review = JsonUtil.readFromJson("distributedlogs_allow_request/delete.json",
          StackGresDistributedLogsReview.class);

        givenAReviewDelete_itShouldFailIfIsAClusterDependsOnIt(review);

    }

    @Override
    @Test
    protected void givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt() throws ValidationFailed {

      StackGresDistributedLogsReview review = JsonUtil.readFromJson("distributedlogs_allow_request/delete.json",
          StackGresDistributedLogsReview.class);

        givenAReviewDelete_itShouldNotFailIfNotClusterDependsOnIt(review);

    }
}
