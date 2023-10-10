/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class RestoreConfigValidator
    extends AbstractReferenceValidator<
      StackGresCluster, StackGresClusterReview, StackGresBackup>
    implements ClusterValidator {

  private final String errorPostgresMismatch = ErrorType
      .getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
  private final String errorConstraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  private final CustomResourceFinder<StackGresBackup> backupFinder;

  @Inject
  public RestoreConfigValidator(CustomResourceFinder<StackGresBackup> backupFinder) {
    super(backupFinder);
    this.backupFinder = backupFinder;
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();

    Optional<StackGresClusterRestore> restoreOpt = Optional.ofNullable(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getInitialData)
        .map(StackGresClusterInitialData::getRestore);

    checkRestoreConfig(review, restoreOpt);

    if (restoreOpt.isPresent()) {
      StackGresClusterRestore restoreConfig = restoreOpt.get();
      checkBackup(review, restoreConfig);
    }
  }

  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  private void checkBackup(
      StackGresClusterReview review,
      StackGresClusterRestore restoreConfig) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    String backupName = restoreConfig.getFromBackup().getName();
    String namespace = cluster.getMetadata().getNamespace();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        if (restoreConfig.getFromBackup() == null) {
          break;
        }
        if (restoreConfig.getFromBackup().getUid() != null) {
          final String message = "uid is deprecated, use name instead!";
          fail(errorConstraintViolationUri, message);
        }

        super.validate(review);

        Optional<StackGresBackup> foundBackup = backupFinder
            .findByNameAndNamespace(backupName, namespace);

        if (foundBackup
            .map(StackGresBackup::getStatus)
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getStatus)
            .map(BackupStatus.COMPLETED.status()::equals)
            .map(completed -> !completed)
            .orElse(true)) {
          final String message = "Cannot restore from SGBackup " + backupName
              + " because it's not ready";
          fail(message);
        }

        if (foundBackup.isPresent()) {
          String backupMajorVersion = getMajorVersion(foundBackup.get());

          String givenPgVersion = review.getRequest().getObject().getSpec()
              .getPostgres().getVersion();
          String givenMajorVersion = getPostgresFlavorComponent(cluster)
              .get(cluster)
              .getMajorVersion(givenPgVersion);

          if (!backupMajorVersion.equals(givenMajorVersion)) {
            final String message = "Cannot restore from SGBackup " + backupName
                + " because it comes from an incompatible postgres version";
            fail(errorPostgresMismatch, message);
          }
        }
        break;
      case UPDATE:
        StackGresClusterRestore oldRestoreConfig = review.getRequest()
            .getOldObject().getSpec().getInitialData().getRestore();

        final String message = "Cannot update SGCluster's restore configuration";
        if (!Objects.equals(restoreConfig, oldRestoreConfig)) {
          fail(errorConstraintViolationUri, message);
        }
        break;
      default:
    }
  }

  private void checkRestoreConfig(
      StackGresClusterReview review,
      Optional<StackGresClusterRestore> initRestoreOpt) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      Optional<StackGresClusterRestore> oldRestoreOpt = Optional
          .ofNullable(review.getRequest().getOldObject().getSpec().getInitialData())
          .map(StackGresClusterInitialData::getRestore);

      if (!initRestoreOpt.isPresent() && oldRestoreOpt.isPresent()) {
        fail(errorConstraintViolationUri, "Cannot update SGCluster's restore configuration");
      }
    }
  }

  private String getMajorVersion(StackGresBackup backup) {
    return backup.getStatus().getBackupInformation().getPostgresVersion().substring(0, 2);
  }

  @Override
  protected Class<StackGresBackup> getReferenceClass() {
    return StackGresBackup.class;
  }

  @Override
  protected String getReference(StackGresCluster resource) {
    return Optional.ofNullable(resource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getInitialData)
        .map(StackGresClusterInitialData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getName)
        .orElse(null);
  }

  @Override
  protected void onNotFoundReference(String message) throws ValidationFailed {
    fail(message);
  }

}
