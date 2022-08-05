/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupInformation;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.BackupReview;
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
class ClusterReferenceValidatorTest {

  private ClusterValidator validator;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  @Mock
  private CustomResourceFinder<StackGresBackup> backupFinder;

  private StackGresCluster cluster;

  private StackGresBackup backup;

  @BeforeEach
  void setUp() throws Exception {
    validator = new ClusterValidator(clusterFinder, backupFinder);
    cluster = Fixtures.cluster().loadDefault().get();
    backup = new StackGresBackup();
  }

  @Test
  void givenAClusterWithInvalidBackupConfigReferenceOnCreation_shouldFail()
      throws ValidationFailed {

    final BackupReview review = AdmissionReviewFixtures.backup().loadCreate().get();
    review.getRequest().getObject().getStatus().setBackupConfig(null);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(clusterFinder.findByNameAndNamespace(clusterName, namespace))
        .thenReturn(Optional.of(cluster));
    when(backupFinder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.ofNullable(null));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cluster " + clusterName + " has no backup configuration", resultMessage);
  }

  @Test
  void givenAClusterWithNoBackupConfigReferenceOnCreation_shouldFail() throws ValidationFailed {

    final BackupReview review = AdmissionReviewFixtures.backup().loadCreate().get();
    review.getRequest().getObject().getStatus().setBackupConfig(null);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    cluster.getSpec().getConfiguration().setBackupConfig(null);

    when(clusterFinder.findByNameAndNamespace(clusterName, namespace))
        .thenReturn(Optional.of(cluster));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cluster " + clusterName + " has no backup configuration", resultMessage);
  }

  @Test
  void givenValidStackGresReferenceOnCreation_shouldNotFail() throws ValidationFailed {

    final BackupReview review = AdmissionReviewFixtures.backup().loadCreate().get();
    review.getRequest().getObject().getStatus().setBackupConfig(null);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(clusterFinder.findByNameAndNamespace(clusterName, namespace))
        .thenReturn(Optional.of(cluster));
    when(backupFinder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backup));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(namespace));

  }

  @Test
  void givenComposedStackGresReferenceOnCreationWithRequiredStatus_shouldNotFail()
      throws ValidationFailed {

    final BackupReview review = AdmissionReviewFixtures.backup().loadCreate().get();
    review.getRequest().getObject().getSpec().setSgCluster(
        StackGresUtil.getRelativeId(
            cluster.getMetadata().getName(),
            cluster.getMetadata().getNamespace(),
            review.getRequest().getObject().getMetadata().getNamespace()));
    review.getRequest().getObject().getStatus().getProcess()
        .setStatus(BackupPhase.COMPLETED.label());
    review.getRequest().getObject().getStatus()
        .setBackupInformation(new StackGresBackupInformation());
    review.getRequest().getObject().getStatus()
        .setInternalName("test");

    validator.validate(review);
  }

  @Test
  void giveInvalidStackGresReferenceOnCreation_shouldFail() {

    final BackupReview review = AdmissionReviewFixtures.backup().loadCreate().get();
    review.getRequest().getObject().getStatus().setBackupConfig(null);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(clusterFinder.findByNameAndNamespace(clusterName, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Cluster " + clusterName + " not found", resultMessage);

    verify(clusterFinder).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void giveInvalidStackGresReferenceOnCreationWithStatusBackupConfig_shouldNotFail()
      throws ValidationFailed {

    final BackupReview review = AdmissionReviewFixtures.backup().loadCreate().get();

    String clusterName =
        review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(eq(clusterName), eq(namespace));
  }

  @Test
  void giveAnAttemptToUpdateReferencedCluster_shouldFail() {

    final BackupReview review = AdmissionReviewFixtures.backup().loadUpdate().get();
    review.getRequest().getObject().getStatus().setBackupConfig(null);

    review.getRequest().getObject().getSpec().setSgCluster("test");

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Backup cluster can not be updated.", resultMessage);

    verify(clusterFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void giveAnAttemptToUpdateManagedLifecycle_shouldNotFail() throws ValidationFailed {

    final BackupReview review = AdmissionReviewFixtures.backup().loadUpdate().get();
    review.getRequest().getObject().getStatus().setBackupConfig(null);

    review.getRequest().getObject().getSpec().setManagedLifecycle(
        !review.getRequest().getObject().getSpec().getManagedLifecycle());

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final BackupReview review = AdmissionReviewFixtures.backup().loadCreate().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }

}
