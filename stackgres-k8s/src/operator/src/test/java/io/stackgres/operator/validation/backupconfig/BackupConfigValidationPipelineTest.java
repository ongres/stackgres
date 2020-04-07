/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;
import io.stackgres.operator.validation.backup.BackupValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@QuarkusTest
@DisabledIfEnvironmentVariable(named = "SKIP_QUARKUS_TEST", matches = "true")
public class BackupConfigValidationPipelineTest
    extends ValidationPipelineTest<StackGresBackupConfig, BackupConfigReview> {

  @Inject
  public BackupConfigValidationPipeline pipeline;

  @Override
  public BackupConfigReview getConstraintViolatingReview() {
    final BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/create.json",
        BackupConfigReview.class);
    review.getRequest().getObject().getSpec().getBaseBackups().setRetention(0);
    return review;
  }

  @Override
  public ValidationPipeline<BackupConfigReview> getPipeline() {
    return pipeline;
  }
}