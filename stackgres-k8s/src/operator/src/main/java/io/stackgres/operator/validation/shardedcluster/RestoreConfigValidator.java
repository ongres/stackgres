/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupProcess;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterInitialData;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestore;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestoreFromBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class RestoreConfigValidator
    extends AbstractReferenceValidator<
      StackGresShardedCluster, StackGresShardedClusterReview, StackGresShardedBackup>
    implements ShardedClusterValidator {

  private final String errorCrReferencerUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);
  private final String errorPostgresMismatch = ErrorType
      .getErrorTypeUri(ErrorType.PG_VERSION_MISMATCH);
  private final String errorConstraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  private final CustomResourceFinder<StackGresShardedBackup> backupFinder;

  @Inject
  public RestoreConfigValidator(CustomResourceFinder<StackGresShardedBackup> backupFinder) {
    super(backupFinder);
    this.backupFinder = backupFinder;
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    StackGresShardedCluster cluster = review.getRequest().getObject();

    Optional<StackGresShardedClusterRestore> restoreOpt = Optional.ofNullable(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getInitialData)
        .map(StackGresShardedClusterInitialData::getRestore);

    checkRestoreConfig(review, restoreOpt);

    if (restoreOpt.isPresent()) {
      StackGresShardedClusterRestore restoreConfig = restoreOpt.get();
      checkBackup(review, restoreConfig);
    }
  }

  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  private void checkBackup(
      StackGresShardedClusterReview review,
      StackGresShardedClusterRestore restoreConfig) throws ValidationFailed {
    StackGresShardedCluster cluster = review.getRequest().getObject();
    String backupName = restoreConfig.getFromBackup().getName();
    String namespace = cluster.getMetadata().getNamespace();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        if (restoreConfig.getFromBackup() == null) {
          break;
        }

        super.validate(review);

        Optional<StackGresShardedBackup> foundBackup = backupFinder
            .findByNameAndNamespace(backupName, namespace);

        if (foundBackup
            .map(StackGresShardedBackup::getStatus)
            .map(StackGresShardedBackupStatus::getProcess)
            .map(StackGresShardedBackupProcess::getStatus)
            .map(ShardedBackupStatus.COMPLETED.status()::equals)
            .map(completed -> !completed)
            .orElse(true)) {
          final String message = "Cannot restore from SGShardedBackup " + backupName
              + " because it's not ready";
          fail(errorCrReferencerUri, message);
        }

        int clusters = 1 + cluster.getSpec().getShards().getClusters();
        if (!foundBackup
            .map(StackGresShardedBackup::getStatus)
            .map(StackGresShardedBackupStatus::getSgBackups)
            .map(sgBackups -> sgBackups.size() == clusters)
            .orElse(false)) {
          fail(
              errorConstraintViolationUri,
              "sgBackups must be an array of size " + clusters
              + " (the coordinator plus the number of shards)"
              + " but was "
              + foundBackup
              .map(StackGresShardedBackup::getStatus)
              .map(StackGresShardedBackupStatus::getSgBackups)
              .map(List::size)
              .map(String::valueOf)
              .orElse("null"));
        }

        if (foundBackup.isPresent()) {
          String backupMajorVersion = getMajorVersion(foundBackup.get());

          String givenPgVersion = review.getRequest().getObject().getSpec()
              .getPostgres().getVersion();
          String givenMajorVersion = getPostgresFlavorComponent(cluster)
              .get(cluster)
              .getMajorVersion(givenPgVersion);

          if (!backupMajorVersion.equals(givenMajorVersion)) {
            final String message = "Cannot restore from SGShardedBackup " + backupName
                + " because it comes from a different postgres major version";
            fail(errorPostgresMismatch, message);
          }
        }
        break;
      case UPDATE:
        StackGresShardedClusterRestore oldRestoreConfig = review.getRequest()
            .getOldObject().getSpec().getInitialData().getRestore();

        final String message = "Cannot update SGShardedCluster's restore configuration";
        if (!Objects.equals(restoreConfig, oldRestoreConfig)) {
          fail(errorConstraintViolationUri, message);
        }
        break;
      default:
    }
  }

  private void checkRestoreConfig(
      StackGresShardedClusterReview review,
      Optional<StackGresShardedClusterRestore> initRestoreOpt) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      Optional<StackGresShardedClusterRestore> oldRestoreOpt = Optional
          .ofNullable(review.getRequest().getOldObject().getSpec().getInitialData())
          .map(StackGresShardedClusterInitialData::getRestore);

      if (!initRestoreOpt.isPresent() && oldRestoreOpt.isPresent()) {
        fail(errorConstraintViolationUri, "Cannot update SGShardedCluster's restore configuration");
      }
    }
  }

  private String getMajorVersion(StackGresShardedBackup backup) {
    return backup.getStatus().getBackupInformation().getPostgresVersion().split("\\.")[0];
  }

  @Override
  protected Class<StackGresShardedBackup> getReferenceClass() {
    return StackGresShardedBackup.class;
  }

  @Override
  protected String getReference(StackGresShardedCluster resource) {
    return Optional.ofNullable(resource.getSpec().getInitialData())
        .map(StackGresShardedClusterInitialData::getRestore)
        .map(StackGresShardedClusterRestore::getFromBackup)
        .map(StackGresShardedClusterRestoreFromBackup::getName)
        .orElse(null);
  }

  @Override
  protected void onNotFoundReference(String message) throws ValidationFailed {
    fail(message);
  }

}
