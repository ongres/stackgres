/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class BackupConfigValidator implements ClusterValidator {

  private CustomResourceFinder<StackGresBackupConfig> configFinder;

  private ConfigContext context;

  @Inject
  public BackupConfigValidator(
      CustomResourceFinder<StackGresBackupConfig> configFinder, ConfigContext context) {
    this.configFinder = configFinder;
    this.context = context;
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String backupConfig = cluster.getSpec().getConfigurations().getBackupConfig();

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

  private void checkIfBackupConfigExists(StackGresClusterReview review,
                                         String onError) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String backupConfig = cluster.getSpec().getConfigurations().getBackupConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    if (backupConfig != null) {
      Optional<StackGresBackupConfig> backupConfigOpt = configFinder
          .findByNameAndNamespace(backupConfig, namespace);

      if (!backupConfigOpt.isPresent()) {
        fail(context, onError);
      }
    }
  }

}
