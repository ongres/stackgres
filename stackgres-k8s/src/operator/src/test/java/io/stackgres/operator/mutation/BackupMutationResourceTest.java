/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupMutationResourceTest extends MutationResourceTest<StackGresBackup, BackupReview> {

  @Override
  protected MutationResource<StackGresBackup, BackupReview> getResource() {
    return new BackupMutationResource(pipeline);
  }

  @Override
  protected BackupReview getReview() {
    return AdmissionReviewFixtures.backup().loadCreate().get();
  }

}
