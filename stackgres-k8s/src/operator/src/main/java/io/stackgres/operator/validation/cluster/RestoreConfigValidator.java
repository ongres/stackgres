/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterRestore;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@ApplicationScoped
public class RestoreConfigValidator implements ClusterValidator {

  private KubernetesCustomResourceScanner<StackGresBackup> backupScanner;

  @Inject
  public RestoreConfigValidator(KubernetesCustomResourceScanner<StackGresBackup> backupScanner) {
    this.backupScanner = backupScanner;
  }

  @Override
  public void validate(StackgresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    StackGresClusterRestore restoreConfig = cluster.getSpec().getRestore();

    checkRestoreConfig(review, restoreConfig);

    if (restoreConfig != null) {
      checkBackup(review, restoreConfig);
    }

  }

  private void checkBackup(StackgresClusterReview review,
                           StackGresClusterRestore restoreConfig) throws ValidationFailed {
    String stackgresBackup = restoreConfig.getStackgresBackup();

    switch (review.getRequest().getOperation()) {
      case CREATE:

        Optional<StackGresBackup> config = findBackup(stackgresBackup);

        if (!config.isPresent()) {
          throw new ValidationFailed("Backup uid " + stackgresBackup + " not found");
        }

        StackGresBackup backup = config.get();

        if (backup.getStatus() == null || !backup.getStatus().getPhase().equals("Completed")) {
          throw new ValidationFailed("Cannot restore from backup " + stackgresBackup
              + " because it's not ready");
        }

        String backupMajorVersion = RestoreConfigValidator.getMajorVersion(backup);

        String givenPgVersion = review.getRequest().getObject().getSpec().getPostgresVersion();
        String calculatedPgVersion = StackGresComponents.calculatePostgresVersion(givenPgVersion);
        String givenMajorVersion = StackGresComponents.getPostgresMajorVersion(calculatedPgVersion);

        if (!backupMajorVersion.equals(givenMajorVersion)) {
          throw new ValidationFailed("Cannot restore from backup " + stackgresBackup
              + " because it comes from an incompatible postgres version");
        }

        break;
      case UPDATE:
        StackGresClusterRestore oldRestoreConfig = review.getRequest()
            .getOldObject().getSpec().getRestore();
        String oldStackgresBackup = oldRestoreConfig.getStackgresBackup();

        if (stackgresBackup == null && oldStackgresBackup != null
            || stackgresBackup != null && oldStackgresBackup == null) {
          throw new ValidationFailed("Cannot update cluster's restore configuration");
        }
        if (stackgresBackup != null && !stackgresBackup.equals(oldStackgresBackup)) {
          throw new ValidationFailed("Cannot update cluster's restore configuration");
        }
        break;
      default:
    }
  }

  private static String getMajorVersion(StackGresBackup backup) {
    return backup.getStatus().getPgVersion().substring(0, 2);
  }

  private void checkRestoreConfig(StackgresClusterReview review,
                                  StackGresClusterRestore restoreConfig) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      StackGresClusterRestore oldRestoreConfig = review.getRequest()
          .getOldObject().getSpec().getRestore();
      if (restoreConfig == null && oldRestoreConfig != null) {
        throw new ValidationFailed("Cannot update cluster's restore configuration");
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
