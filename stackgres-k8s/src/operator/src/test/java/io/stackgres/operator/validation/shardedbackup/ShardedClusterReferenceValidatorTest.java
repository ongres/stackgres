/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupInformation;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfigurationBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class ShardedClusterReferenceValidatorTest {

  private ShardedClusterValidator validator;

  @Mock
  private CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private StackGresShardedCluster cluster;

  private StackGresShardedClusterBackupConfiguration backup;

  @BeforeEach
  void setUp() throws Exception {
    validator = new ShardedClusterValidator(clusterFinder);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    cluster.getSpec().getConfigurations().setBackups(List.of(
        new StackGresShardedClusterBackupConfigurationBuilder()
        .withSgObjectStorage("test")
        .build()));
    backup = new StackGresShardedClusterBackupConfiguration();
  }

  @Test
  void givenAClusterWithNoBackupConfigReferenceOnCreation_shouldFail() throws ValidationFailed {
    final ShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    review.getRequest().getObject().getStatus().setSgBackups(null);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    cluster.getSpec().getConfigurations().setBackups(null);

    when(clusterFinder.findByNameAndNamespace(clusterName, namespace))
        .thenReturn(Optional.of(cluster));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGShardedCluster " + clusterName + " has no backup configuration", resultMessage);
  }

  @Test
  void givenValidStackGresReferenceOnCreation_shouldNotFail() throws ValidationFailed {
    final ShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    review.getRequest().getObject().getStatus().setSgBackups(null);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getConfigurations().setBackups(Arrays.asList(backup));

    when(clusterFinder.findByNameAndNamespace(clusterName, namespace))
        .thenReturn(Optional.of(cluster));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(namespace));
  }

  @Test
  void givenComposedStackGresReferenceOnCreationWithRequiredStatus_shouldNotFail()
      throws ValidationFailed {
    final ShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    review.getRequest().getObject().getSpec().setSgShardedCluster(
        StackGresUtil.getRelativeId(
            cluster.getMetadata().getName(),
            cluster.getMetadata().getNamespace(),
            review.getRequest().getObject().getMetadata().getNamespace()));
    review.getRequest().getObject().getStatus().getProcess()
        .setStatus(ShardedBackupStatus.COMPLETED.status());
    review.getRequest().getObject().getStatus()
        .setBackupInformation(new StackGresShardedBackupInformation());

    validator.validate(review);
  }

  @Test
  void giveInvalidStackGresReferenceOnCreation_shouldFail() {
    final ShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    review.getRequest().getObject().getStatus().setSgBackups(null);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(clusterFinder.findByNameAndNamespace(clusterName, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGShardedCluster " + clusterName + " not found", resultMessage);

    verify(clusterFinder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveInvalidStackGresReferenceOnCreationWithStatusBackupConfig_shouldNotFail()
      throws ValidationFailed {
    final ShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadCreate().get();

    String clusterName =
        review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(eq(clusterName), eq(namespace));
  }

  @Test
  void giveAnAttemptToUpdateReferencedCluster_shouldFail() {
    final ShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();

    review.getRequest().getObject().getSpec().setSgShardedCluster("test");

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Backup sgShardedCluster can not be updated.", resultMessage);

    verify(clusterFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToUpdateManagedLifecycle_shouldNotFail() throws ValidationFailed {
    final ShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();

    review.getRequest().getObject().getSpec().setManagedLifecycle(
        !review.getRequest().getObject().getSpec().getManagedLifecycle());

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {
    final ShardedBackupReview review = AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

}
