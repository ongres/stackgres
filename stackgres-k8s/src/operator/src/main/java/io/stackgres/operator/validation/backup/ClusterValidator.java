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
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

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
      case CREATE -> {
        StackGresBackup backup = review.getRequest().getObject();
        String clusterName = backup.getSpec().getSgCluster();
        if (Optional.of(backup)
            .map(StackGresBackup::getStatus)
            .map(StackGresBackupStatus::getBackupConfig)
            .isEmpty()) {
          checkIfClusterExists(review, "Cluster " + clusterName + " not found");
        }
      }
      case UPDATE -> {
        StackGresBackup backup = review.getRequest().getObject();
        String cluster = backup != null ? backup.getSpec().getSgCluster() : null;
        if (!review.getRequest().getOldObject().getSpec().getSgCluster()
            .equals(cluster)) {
          final String message = "Backup cluster can not be updated.";
          fail(message);
        }
      }
      default -> {
      }
    }

  }

  private void checkIfClusterExists(BackupReview review,
                                    String onError) throws ValidationFailed {

    var location = getClusterLocation(review);

    Optional<StackGresCluster> clusterOpt = clusterFinder
        .findByNameAndNamespace(location.v2, location.v1);

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
