/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class ClusterValidator implements BackupValidator {

  private final KubernetesCustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public ClusterValidator(
      KubernetesCustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
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
          throw new ValidationFailed("Backup cluster can not be updated.");
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
      throw new ValidationFailed(onError);
    }
  }

}
