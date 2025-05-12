/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.KubernetesTestServerSetup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ValidationPipelineTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
@EnabledIfEnvironmentVariable(named = "QUARKUS_PROFILE", matches = "test")
public class ShardedBackupValidationPipelineTest
    extends ValidationPipelineTest<StackGresShardedBackup, StackGresShardedBackupReview> {

  @Inject
  public ShardedBackupValidationPipeline pipeline;

  @Override
  public StackGresShardedBackupReview getConstraintViolatingReview() {
    final StackGresShardedBackupReview backupReview =
        AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    backupReview.getRequest().getObject().getSpec().setSgShardedCluster(null);
    return backupReview;
  }

  @Override
  public ShardedBackupValidationPipeline getPipeline() {
    return pipeline;
  }
}
