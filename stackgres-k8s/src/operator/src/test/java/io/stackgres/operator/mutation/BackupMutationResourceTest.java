/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.StackGresBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupMutationResourceTest extends MutationResourceTest<StackGresBackup, StackGresBackupReview> {

  @Override
  protected AbstractMutationResource<StackGresBackup, StackGresBackupReview> getResource() {
    return new BackupMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresBackupReview getReview() {
    return AdmissionReviewFixtures.backup().loadCreate().get();
  }

}
