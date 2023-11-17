/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import static java.util.Optional.ofNullable;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.resource.NamedResource;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ClusterValidator implements BackupValidator {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);

  @Inject
  public ClusterValidator(
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    StackGresBackup backup = review.getRequest().getObject();

    switch (review.getRequest().getOperation()) {
      case CREATE -> {
        validateCluster(backup, review, backup.getSpec().getSgCluster());
      }
      case UPDATE -> {
        if (!review.getRequest().getOldObject().getSpec().getSgCluster()
            .equals(backup.getSpec().getSgCluster())) {
          final String message = "Backup sgCluster can not be updated.";
          fail(message);
        }
      }
      default -> {
      }
    }
  }

  private void validateCluster(StackGresBackup backup, BackupReview review, String clusterName)
      throws ValidationFailed {
    if (hasStatusBackupConfig(backup)) {
      return;
    }

    NamedResource namedResource = getClusterLocation(review);
    Optional<StackGresCluster> clusterOpt = clusterFinder
        .findByNameAndNamespace(namedResource.resource(), namedResource.namespace());

    checkIfClusterExists(clusterOpt, "SGCluster " + clusterName + " not found");
    checkIfClusterHasValidBackupConfig(clusterOpt,
        "SGCluster " + clusterName + " has no backup configuration");
  }

  private void checkIfClusterHasValidBackupConfig(Optional<StackGresCluster> clusterOpt,
      String onError) throws ValidationFailed {
    var backupConfig = ofNullable(
        clusterOpt.get().getSpec().getConfigurations().getBackups());

    if (backupConfig.isEmpty() || backupConfig.get().isEmpty()) {
      fail(onError);
    }

  }

  private boolean hasStatusBackupConfig(StackGresBackup backup) {
    return Optional.of(backup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getSgBackupConfig)
        .isPresent();
  }

  private void checkIfClusterExists(Optional<StackGresCluster> clusterOpt, String onError)
      throws ValidationFailed {
    if (clusterOpt.isEmpty()) {
      fail(onError);
    }
  }

  private NamedResource getClusterLocation(BackupReview review) {
    StackGresBackup backup = review.getRequest().getObject();
    String cluster = backup.getSpec().getSgCluster();
    if (cluster.contains(".")) {
      String[] clusterLocation = cluster.split("\\.");
      return new NamedResource(clusterLocation[0], clusterLocation[1]);
    } else {
      String namespace = review.getRequest().getObject().getMetadata().getNamespace();
      return new NamedResource(namespace, cluster);
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }

}
