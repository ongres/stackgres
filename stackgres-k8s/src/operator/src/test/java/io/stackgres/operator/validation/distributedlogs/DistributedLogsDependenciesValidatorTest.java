/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
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
class DistributedLogsDependenciesValidatorTest extends DependenciesValidatorTest
        <StackGresDistributedLogsReview, DistributedLogsDependenciesValidator> {

  @Override
  protected DependenciesValidator<StackGresDistributedLogsReview> setUpValidation() {
    return new DistributedLogsDependenciesValidator();
  }

  @Override
  protected StackGresDistributedLogsReview getReview_givenAReviewCreation_itShouldDoNothing() {
    return JsonUtil.readFromJson("distributedlogs_allow_request/create.json",
        StackGresDistributedLogsReview.class);
  }

  @Override
  protected StackGresDistributedLogsReview getReview_givenAReviewUpdate_itShouldDoNothing() {
    return JsonUtil.readFromJson("distributedlogs_allow_request/update.json",
        StackGresDistributedLogsReview.class);
  }

  @Override
  protected StackGresDistributedLogsReview
      getReview_givenAReviewDelete_itShouldFailIfAClusterDependsOnIt() {
    return JsonUtil.readFromJson("distributedlogs_allow_request/delete.json",
        StackGresDistributedLogsReview.class);
  }

  @Override
  protected StackGresDistributedLogsReview
      getReview_givenAReviewDelete_itShouldNotFailIfNoClusterDependsOnIt()
      throws ValidationFailed {
    return JsonUtil.readFromJson("distributedlogs_allow_request/delete.json",
        StackGresDistributedLogsReview.class);
  }

  @Override
  protected StackGresDistributedLogsReview
      getReview_givenAReviewDelete_itShouldNotFailIfNoClusterExists() {
    return JsonUtil.readFromJson("distributedlogs_allow_request/delete.json",
        StackGresDistributedLogsReview.class);
  }

  @Override
  protected void makeClusterNotDependant(StackGresCluster cluster) {
    cluster.getSpec().setDistributedLogs(null);
  }
}
