/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class ShardedBackupValidationPipelineTest
    extends ValidationPipelineTest<StackGresShardedBackup, ShardedBackupReview> {

  @Inject
  public ShardedBackupValidationPipeline pipeline;

  @Override
  public ShardedBackupReview getConstraintViolatingReview() {
    final ShardedBackupReview backupReview =
        AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    backupReview.getRequest().getObject().getSpec().setSgShardedCluster(null);
    return backupReview;
  }

  @Override
  public ShardedBackupValidationPipeline getPipeline() {
    return pipeline;
  }
}
