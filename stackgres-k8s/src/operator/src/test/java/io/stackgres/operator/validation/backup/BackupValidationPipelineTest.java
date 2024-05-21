/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.StackGresBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class BackupValidationPipelineTest
    extends ValidationPipelineTest<StackGresBackup, StackGresBackupReview> {

  @Inject
  public BackupValidationPipeline pipeline;

  @Override
  public StackGresBackupReview getConstraintViolatingReview() {
    final StackGresBackupReview backupReview = AdmissionReviewFixtures.backup().loadCreate().get();
    backupReview.getRequest().getObject().getSpec().setSgCluster(null);
    return backupReview;
  }

  @Override
  public BackupValidationPipeline getPipeline() {
    return pipeline;
  }
}
