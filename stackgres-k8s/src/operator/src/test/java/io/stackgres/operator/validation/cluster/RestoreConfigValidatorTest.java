/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.ClusterRestore;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestoreConfigValidatorTest {

  @Mock
  private CustomResourceScanner<StackGresBackup> scanner;

  private RestoreConfigValidator validator;

  private StackGresBackupList backupList;


  @BeforeEach
  void setUp() {

    validator = new RestoreConfigValidator(scanner);
    backupList = JsonUtil
        .readFromJson("backup/list.json", StackGresBackupList.class);
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {

    final StackGresClusterReview review = getCreationReview();

    when(scanner.findResources())
        .thenReturn(Optional.of(backupList.getItems()));

    validator.validate(review);

    verify(scanner).findResources();

  }

  @Test
  void givenAInvalidCreation_shouldFail() {

    final StackGresClusterReview review = getCreationReview();

    StackGresCluster cluster = review.getRequest().getObject();
    ClusterRestore restoreConfig = cluster.getSpec().getRestore();
    String stackgresBackup = restoreConfig.getBackupUid();

    when(scanner.findResources()).thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Backup uid " + stackgresBackup + " not found");

    verify(scanner).findResources();

  }

  @Test
  void givenACreationWithBackupFromDifferentPgVersion_shouldFail() throws ValidationFailed {

    final StackGresClusterReview review = getCreationReview();
    String stackgresBackup = review.getRequest()
        .getObject().getSpec().getRestore().getBackupUid();

    StackGresBackup backup = backupList.getItems().stream()
        .filter(b -> b.getMetadata().getUid().equals(stackgresBackup))
        .findFirst().orElseThrow(AssertionError::new);

    backup.getStatus().setPgVersion("120001");

    when(scanner.findResources())
        .thenReturn(Optional.of(backupList.getItems()));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot restore from backup " + stackgresBackup
            + " because it comes from an incompatible postgres version");


    verify(scanner).findResources();

  }

  @Test
  void givenACreationWithNoRestoreConfig_shouldDoNothing() throws ValidationFailed {

    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().setRestore(null);

    validator.validate(review);

    verify(scanner, never()).findResources(anyString());

  }

  @Test
  void givenAnUpdate_shouldFail() {

    final StackGresClusterReview review = getUpdateReview();

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update cluster's restore configuration");

    verify(scanner, never()).findResources();

  }

  private StackGresClusterReview getCreationReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackGresClusterReview.class);
  }

  private StackGresClusterReview getUpdateReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/restore_config_update.json",
            StackGresClusterReview.class);
  }

}