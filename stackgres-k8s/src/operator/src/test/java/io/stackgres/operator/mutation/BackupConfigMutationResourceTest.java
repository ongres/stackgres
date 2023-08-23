/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigMutationResourceTest
    extends MutationResourceTest<StackGresBackupConfig, BackupConfigReview> {

  @Override
  protected AbstractMutationResource<StackGresBackupConfig, BackupConfigReview> getResource() {
    return new BackupConfigMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected BackupConfigReview getReview() {
    return AdmissionReviewFixtures.backupConfig().loadCreate().get();
  }

}
