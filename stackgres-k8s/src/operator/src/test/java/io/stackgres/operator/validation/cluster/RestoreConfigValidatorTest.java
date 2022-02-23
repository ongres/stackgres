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
import io.stackgres.common.resource.CustomResourceFinder;
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
  private CustomResourceFinder<StackGresBackup> finder;

  private RestoreConfigValidator validator;

  private StackGresBackupList backupList;

  @BeforeEach
  void setUp() {

    validator = new RestoreConfigValidator(finder);
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
    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backupList.getItems().get(0)));

    validator.validate(review);

    verify(finder).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void givenAInvalidCreation_shouldFail() {

    final StackGresClusterReview review = getCreationReview();

    StackGresCluster cluster = review.getRequest().getObject();
    StackGresClusterRestore restoreConfig = cluster.getSpec().getInitData().getRestore();
    String backupName = restoreConfig.getFromBackup().getName();

    when(finder.findByNameAndNamespace(anyString(), anyString())).thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Backup name " + backupName + " not found");

    verify(finder).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void givenACreationWithBackupFromDifferentPgVersion_shouldFail() {

    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(secondPgMajorVersion);
    String backupName = review.getRequest()
        .getObject().getSpec().getInitData().getRestore().getFromBackup().getName();

    StackGresBackup backup = backupList.getItems().stream()
        .filter(b -> b.getMetadata().getName().equals(backupName))
        .findFirst().orElseThrow(AssertionError::new);

    backup.getStatus().getBackupInformation().setPostgresVersion(
        firstPgMajorVersionNumber);

    when(finder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(backupList.getItems().get(0)));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot restore from backup " + backupName
            + " because it comes from an incompatible postgres version");

    verify(finder).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void givenACreationWithBackupFromAndUid_shouldFail() {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getInitData().getRestore()
        .getFromBackup().setName(null);
    review.getRequest().getObject().getSpec().getInitData().getRestore()
        .getFromBackup().setUid("23442867-377d-11ea-b04b-0242ac110004");

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "uid is deprecated, use name instead!");

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenACreationWithNoRestoreConfig_shouldDoNothing() throws ValidationFailed {

    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getInitData().setRestore(null);

    validator.validate(review);

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());

  }

  @Test
  void givenAnUpdate_shouldFail() {

    final StackGresClusterReview review = getUpdateReview();

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update cluster's restore configuration");

    verify(finder, never()).findByNameAndNamespace(anyString(), anyString());

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
