/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

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
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresBackup backup = review.getRequest().getObject();
        String clusterName = backup.getSpec().getSgCluster();
        if (Optional.ofNullable(backup)
            .map(StackGresBackup::getStatus)
            .map(StackGresBackupStatus::getBackupConfig)
            .isEmpty()) {
          checkIfClusterExists(review, "Cluster " + clusterName + " not found");
        }
        break;
      }
      case UPDATE: {
        StackGresBackup backup = review.getRequest().getObject();
        String cluster = backup != null ? backup.getSpec().getSgCluster() : null;
        if (!review.getRequest().getOldObject().getSpec().getSgCluster()
            .equals(cluster)) {
          final String message = "Backup cluster can not be updated.";
          fail(message);
        }
        break;
      }
      default:
        break;
    }

  }

  private void checkIfClusterExists(BackupReview review,
                                    String onError) throws ValidationFailed {

    StackGresBackup backup = review.getRequest().getObject();
    String cluster = backup.getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    Optional<StackGresCluster> clusterOpt = clusterFinder
        .findByNameAndNamespace(cluster, namespace);

    if (!clusterOpt.isPresent()) {
      fail(onError);
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }

}
