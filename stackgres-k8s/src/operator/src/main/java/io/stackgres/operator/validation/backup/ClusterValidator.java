/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import static java.util.Optional.ofNullable;

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
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ClusterValidator implements BackupValidator {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final CustomResourceFinder<StackGresBackup> backupFinder;

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);

  @Inject
  public ClusterValidator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresBackup> backupFinder) {
    this.clusterFinder = clusterFinder;
    this.backupFinder = backupFinder;
  }

  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    StackGresBackup backup = review.getRequest().getObject();
    String clusterName = backup != null ? backup.getSpec().getSgCluster() : null;

    switch (review.getRequest().getOperation()) {
      case CREATE -> {
        validateCluster(backup, review, clusterName);
      }
      case UPDATE -> {
        if (!review.getRequest().getOldObject().getSpec().getSgCluster()
            .equals(clusterName)) {
          final String message = "Backup cluster can not be updated.";
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

    var location = getClusterLocation(review);
    var namespace = location.v1;
    var cluster = location.v2;

    Optional<StackGresCluster> clusterOpt = clusterFinder
        .findByNameAndNamespace(cluster, namespace);

    checkIfClusterExists(clusterOpt, "Cluster " + clusterName + " not found");
    checkIfClusterHasValidBackupConfig(clusterOpt, namespace,
        "Cluster " + clusterName + " has no backup configuration");
  }

  private void checkIfClusterHasValidBackupConfig(Optional<StackGresCluster> clusterOpt,
      String namespace, String onError) throws ValidationFailed {

    var backupConfig = ofNullable(
        clusterOpt.get().getSpec().getConfiguration().getBackupConfig());

    if (backupConfig.isEmpty() || backupFinder.findByNameAndNamespace(
        backupConfig.get(), namespace).isEmpty()) {
      fail(onError);
    }

  }

  private boolean hasStatusBackupConfig(StackGresBackup backup) {
    return Optional.of(backup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getBackupConfig)
        .isPresent();
  }

  private void checkIfClusterExists(Optional<StackGresCluster> clusterOpt, String onError)
      throws ValidationFailed {

    if (clusterOpt.isEmpty()) {
      fail(onError);
    }
  }

  private Tuple2<String, String> getClusterLocation(BackupReview review) {
    StackGresBackup backup = review.getRequest().getObject();
    String cluster = backup.getSpec().getSgCluster();
    if (cluster.contains(".")) {
      String[] clusterLocation = cluster.split("\\.");
      return Tuple.tuple(clusterLocation[0], clusterLocation[1]);
    } else {
      String namespace = review.getRequest().getObject().getMetadata().getNamespace();
      return Tuple.tuple(namespace, cluster);
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }

}
