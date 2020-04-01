/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;

@QuarkusTest
class BackupValidationPipelineIt extends ValidationPipelineTest<StackGresBackup, BackupReview> {

  @Override
  public BackupReview getConstraintViolatingReview() {
    final BackupReview backupReview = JsonUtil
        .readFromJson("backup_allow_request/create.json", BackupReview.class);
    backupReview.getRequest().getObject().getSpec().setSgCluster(null);
    return backupReview;
  }
}