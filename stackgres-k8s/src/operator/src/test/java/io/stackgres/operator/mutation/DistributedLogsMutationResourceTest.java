/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsMutationResourceTest
    extends MutationResourceTest<StackGresDistributedLogs, StackGresDistributedLogsReview> {

  @Override
  protected AbstractMutationResource<StackGresDistributedLogs, StackGresDistributedLogsReview>
      getResource() {
    return new DistributedLogsMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresDistributedLogsReview getReview() {
    return AdmissionReviewFixtures.distributedLogs().loadCreate().get();
  }

}
