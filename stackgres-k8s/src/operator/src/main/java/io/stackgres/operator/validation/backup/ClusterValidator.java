/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ClusterValidator implements BackupValidator {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private String errorTypeUri;

  @Inject
  public ClusterValidator(
      CustomResourceFinder<StackGresCluster> clusterFinder, ConfigContext context) {
    this.clusterFinder = clusterFinder;
    errorTypeUri = context.getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);

  }

  @Override
  public void validate(BackupReview review) throws ValidationFailed {

    StackGresBackup backup = review.getRequest().getObject();
    String cluster = backup != null ? backup.getSpec().getCluster() : null;

    switch (review.getRequest().getOperation()) {
      case CREATE:
        checkIfClusterExists(review, "Cluster " + cluster
            + " not found");
        break;
      case UPDATE:
        if (!review.getRequest().getOldObject().getSpec().getCluster()
            .equals(cluster)) {
          final String message = "Backup cluster can not be updated.";
          fail(message);
        }
        break;
      default:
    }

  }

  private void checkIfClusterExists(BackupReview review,
                                    String onError) throws ValidationFailed {

    StackGresBackup backup = review.getRequest().getObject();
    String cluster = backup.getSpec().getCluster();
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
