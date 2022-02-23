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

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
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
  private AbstractCustomResourceFinder<StackGresCluster> clusterFinder;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() throws Exception {
    validator = new ClusterValidator(clusterFinder);

    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

  }

  @Test
  void givenValidStackGresReferenceOnCreation_shouldNotFail() throws ValidationFailed {

    final BackupReview review = JsonUtil
        .readFromJson("backup_allow_request/create.json", BackupReview.class);
    review.getRequest().getObject().getStatus().setBackupConfig(null);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(clusterFinder.findByNameAndNamespace(clusterName, namespace))
        .thenReturn(Optional.of(cluster));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(namespace));

  }

  @Test
  void giveInvalidStackGresReferenceOnCreation_shouldFail() {

    final BackupReview review = JsonUtil
        .readFromJson("backup_allow_request/create.json", BackupReview.class);
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

    final BackupReview review = JsonUtil
        .readFromJson("backup_allow_request/create.json", BackupReview.class);

    String clusterName =
        review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(eq(clusterName), eq(namespace));
  }

  @Test
  void giveAnAttemptToUpdateReferencedCluster_shouldFail() {

    final BackupReview review = JsonUtil
        .readFromJson("backup_allow_request/update.json", BackupReview.class);
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

    final BackupReview review = JsonUtil
        .readFromJson("backup_allow_request/update.json", BackupReview.class);
    review.getRequest().getObject().getStatus().setBackupConfig(null);

    review.getRequest().getObject().getSpec().setManagedLifecycle(
        !review.getRequest().getObject().getSpec().getManagedLifecycle());

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void giveAnAttemptToDelete_shouldNotFail() throws ValidationFailed {

    final BackupReview review = JsonUtil
        .readFromJson("backup_allow_request/create.json", BackupReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(clusterFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }

}
