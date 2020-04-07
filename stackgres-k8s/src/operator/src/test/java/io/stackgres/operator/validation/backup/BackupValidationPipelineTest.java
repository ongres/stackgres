/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ValidationPipelineTest;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@QuarkusTest
@DisabledIfEnvironmentVariable(named = "SKIP_QUARKUS_TEST", matches = "true")
public class BackupValidationPipelineTest extends ValidationPipelineTest<StackGresBackup, BackupReview> {

  @Inject
  public BackupValidationPipeline pipeline;

  @Override
  public BackupReview getConstraintViolatingReview() {
    final BackupReview backupReview = JsonUtil
        .readFromJson("backup_allow_request/create.json", BackupReview.class);
    backupReview.getRequest().getObject().getSpec().setSgCluster(null);
    return backupReview;
  }

  @Override
  public BackupValidationPipeline getPipeline() {
    return pipeline;
  }
}