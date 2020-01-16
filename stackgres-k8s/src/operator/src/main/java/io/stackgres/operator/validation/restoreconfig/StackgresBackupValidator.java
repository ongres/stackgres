/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operatorframework.Operation;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class StackgresBackupValidator implements RestoreConfigValidator {

  private KubernetesCustomResourceScanner<StackGresBackup> backupScanner;

  @Inject
  public StackgresBackupValidator(KubernetesCustomResourceScanner<StackGresBackup> backupScanner) {
    this.backupScanner = backupScanner;
  }

  @Override
  public void validate(RestoreConfigReview review) throws ValidationFailed {

    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {

      String stackgresBackup = review.getRequest().getObject()
          .getSpec().getSource().getStackgresBackup();
      if (stackgresBackup != null) {

        Optional<String> backup = backupScanner.findResources()
            .flatMap(backups -> backups.stream()
                .map(b -> b.getMetadata().getUid())
                .filter(b -> b.equals(stackgresBackup)).findFirst());

        if (!backup.isPresent()) {
          throw new ValidationFailed("Backup " + stackgresBackup + " doesn't exists");
        }
      }
    }

  }
}
