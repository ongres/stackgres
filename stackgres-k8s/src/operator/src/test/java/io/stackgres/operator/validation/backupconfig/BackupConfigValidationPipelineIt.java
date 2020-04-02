/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;
import org.junit.jupiter.api.Disabled;

@QuarkusTest
@Disabled
class BackupConfigValidationPipelineIt
    extends ValidationPipelineTest<StackGresBackupConfig, BackupConfigReview> {

  @Override
  public BackupConfigReview getConstraintViolatingReview() {
    final BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/create.json",
        BackupConfigReview.class);
    review.getRequest().getObject().getSpec().getBaseBackups().setRetention(0);
    return review;
  }
}