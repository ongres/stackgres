/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.validation.StackgresClusterReview;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class BackupConfigValidator implements ClusterValidator {

  private KubernetesCustomResourceFinder<StackGresBackupConfig> configFinder;

  @Inject
  public BackupConfigValidator(
      KubernetesCustomResourceFinder<StackGresBackupConfig> configFinder) {
    this.configFinder = configFinder;
  }

  @Override
  public void validate(StackgresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String backupConfig = cluster.getSpec().getBackupConfig();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        checkIfBackupConfigExists(review, "Backup config " + backupConfig
            + " not found");
        break;
      case UPDATE:
        checkIfBackupConfigExists(review, "Cannot update to backup config "
            + backupConfig + " because it doesn't exists");
        break;
      default:
    }

  }

  private void checkIfBackupConfigExists(StackgresClusterReview review,
                                          String onError) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String backupConfig = cluster.getSpec().getBackupConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    Optional<StackGresBackupConfig> backupConfigOpt = configFinder
        .findByNameAndNamespace(backupConfig, namespace);

    if (!backupConfigOpt.isPresent()) {
      throw new ValidationFailed(onError);
    }
  }

}
