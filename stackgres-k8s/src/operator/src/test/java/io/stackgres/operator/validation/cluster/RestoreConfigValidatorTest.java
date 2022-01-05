/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestoreConfigValidatorTest {

  private static final String firstPgMajorVersion =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions()
          .get(0).get();

  private static final String secondPgMajorVersion =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions()
          .get(1).get();

  private static final String firstPgMajorVersionNumber =
      Seq.of(StackGresComponent.POSTGRESQL.getLatest().findVersion(firstPgMajorVersion)
          .split("\\.")).map(Integer::valueOf).append(1)
          .map(number -> String.format("%02d", number)).toString();

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
    review.getRequest().getObject().getSpec().getPostgres().setVersion(firstPgMajorVersion);

    StackGresBackupList backupList = JsonUtil
        .readFromJson("backup/list.json", StackGresBackupList.class);
    backupList.getItems().get(0).getStatus().getBackupInformation()
        .setPostgresVersion(firstPgMajorVersionNumber);
    when(scanner.findResources())
        .thenReturn(Optional.of(backupList.getItems()));

    validator.validate(review);

    verify(scanner).findResources();

  }

  @Test
  void givenAInvalidCreation_shouldFail() {

    final StackGresClusterReview review = getCreationReview();

    StackGresCluster cluster = review.getRequest().getObject();
    StackGresClusterRestore restoreConfig = cluster.getSpec().getInitData().getRestore();
    String stackgresBackup = restoreConfig.getFromBackup().getUid();

    when(scanner.findResources()).thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Backup uid " + stackgresBackup + " not found");

    verify(scanner).findResources();

  }

  @Test
  void givenACreationWithBackupFromDifferentPgVersion_shouldFail() {

    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(secondPgMajorVersion);
    String stackgresBackup = review.getRequest()
        .getObject().getSpec().getInitData().getRestore().getFromBackup().getUid();

    StackGresBackup backup = backupList.getItems().stream()
        .filter(b -> b.getMetadata().getUid().equals(stackgresBackup))
        .findFirst().orElseThrow(AssertionError::new);

    backup.getStatus().getBackupInformation().setPostgresVersion(
        firstPgMajorVersionNumber);

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
    review.getRequest().getObject().getSpec().getInitData().setRestore(null);

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
