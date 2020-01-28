/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterRestore;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.ValidationFailed;
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
  private KubernetesCustomResourceScanner<StackGresBackup> scanner;

  private RestoreConfigValidator validator;

  private static final StackGresBackupList backupList = JsonUtil
      .readFromJson("backup/list.json", StackGresBackupList.class);


  @BeforeEach
  void setUp() {
    validator = new RestoreConfigValidator(scanner);
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {

    final StackgresClusterReview review = getCreationReview();

    when(scanner.findResources())
        .thenReturn(Optional.of(RestoreConfigValidatorTest.backupList.getItems()));

    validator.validate(review);

    verify(scanner).findResources();

  }

  @Test
  void givenAInvalidCreation_shouldFail() {

    final StackgresClusterReview review = getCreationReview();

    StackGresCluster cluster = review.getRequest().getObject();
    StackGresClusterRestore restoreConfig = cluster.getSpec().getRestore();
    String stackgresBackup = restoreConfig.getStackgresBackup();

    when(scanner.findResources()).thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Backup uid " + stackgresBackup + " not found");

    verify(scanner).findResources();

  }

  @Test
  void givenACreationWithNoRestoreConfig_shouldDoNothing() throws ValidationFailed {

    final StackgresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().setRestore(null);

    validator.validate(review);

    verify(scanner, never()).findResources(anyString());

  }

  @Test
  void givenAnUpdate_shouldFail() {

    final StackgresClusterReview review = getUpdateReview();

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update cluster's restore configuration");

    verify(scanner, never()).findResources();

  }

  private StackgresClusterReview getCreationReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackgresClusterReview.class);
  }

  private StackgresClusterReview getUpdateReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/restore_config_update.json",
            StackgresClusterReview.class);
  }

}