/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class RestoreConfigValidator implements ClusterValidator {

  private final String errorCrReferencerUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);
  private final String errorPostgresMismatch = ErrorType
      .getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);

  private final CustomResourceScanner<StackGresBackup> backupScanner;

  @Inject
  public RestoreConfigValidator(CustomResourceScanner<StackGresBackup> backupScanner) {
    this.backupScanner = backupScanner;
  }

  private static String getMajorVersion(StackGresBackup backup) {
    return backup.getStatus().getBackupInformation().getPostgresVersion().substring(0, 2);
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();

    Optional<StackGresClusterRestore> restoreOpt = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getInitData)
        .map(StackGresClusterInitData::getRestore);

    checkRestoreConfig(review, restoreOpt);

    if (restoreOpt.isPresent()) {
      StackGresClusterRestore restoreConfig = restoreOpt.get();
      checkBackup(review, restoreConfig);
    }
  }

  private void checkBackup(StackGresClusterReview review,
                           StackGresClusterRestore restoreConfig) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    String backupUid = restoreConfig.getFromBackup().getUid();

    switch (review.getRequest().getOperation()) {
      case CREATE:

        Optional<StackGresBackup> config = findBackup(backupUid);

        if (config.isEmpty()) {

          final String message = "Backup uid " + backupUid + " not found";
          fail(errorCrReferencerUri, message);
        }

        StackGresBackup backup = config.get();

        final StackGresBackupProcess process = backup.getStatus().getProcess();
        if (backup.getStatus() == null || !process.getStatus().equals("Completed")) {
          final String message = "Cannot restore from backup " + backupUid
              + " because it's not ready";
          fail(errorCrReferencerUri, message);
        }

        String backupMajorVersion = RestoreConfigValidator.getMajorVersion(backup);

        String givenPgVersion = review.getRequest().getObject().getSpec()
            .getPostgres().getVersion();
        String givenMajorVersion = getPostgresFlavorComponent(cluster)
            .get(cluster)
            .findMajorVersion(givenPgVersion);

        if (!backupMajorVersion.equals(givenMajorVersion)) {
          final String message = "Cannot restore from backup " + backupUid
              + " because it comes from an incompatible postgres version";
          fail(errorPostgresMismatch, message);
        }

        break;
      case UPDATE:
        StackGresClusterRestore oldRestoreConfig = review.getRequest()
            .getOldObject().getSpec().getInitData().getRestore();
        String oldBackupUid = oldRestoreConfig.getFromBackup().getUid();

        final String message = "Cannot update cluster's restore configuration";
        if (backupUid == null && oldBackupUid != null
            || backupUid != null && oldBackupUid == null) {
          fail(errorCrReferencerUri, message);
        }
        if (backupUid != null && !backupUid.equals(oldBackupUid)) {
          fail(errorCrReferencerUri, message);
        }
        break;
      default:
    }
  }

  private void checkRestoreConfig(StackGresClusterReview review,
                                  Optional<StackGresClusterRestore> initRestoreOpt)
      throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {

      Optional<StackGresClusterRestore> oldRestoreOpt = Optional
          .ofNullable(review.getRequest().getOldObject().getSpec().getInitData())
          .map(StackGresClusterInitData::getRestore);

      if (!initRestoreOpt.isPresent() && oldRestoreOpt.isPresent()) {
        fail(errorCrReferencerUri, "Cannot update cluster's restore configuration");
      }
    }
  }

  private Optional<StackGresBackup> findBackup(String stackgresBackup) {
    Optional<List<StackGresBackup>> resources = backupScanner.findResources();
    return resources.flatMap(backups -> backups.stream()
        .filter(b -> b.getMetadata().getUid().equals(stackgresBackup))
        .findFirst());
  }
}
