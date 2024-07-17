/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupMutationResourceTest
    extends MutationResourceTest<StackGresShardedBackup, StackGresShardedBackupReview> {

  @Override
  protected AbstractMutationResource<StackGresShardedBackup, StackGresShardedBackupReview> getResource() {
    return new ShardedBackupMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresShardedBackupReview getReview() {
    return AdmissionReviewFixtures.shardedBackup().loadCreate().get();
  }

}
