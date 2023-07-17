/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import java.util.Optional;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupPerformanceMutator implements BackupMutator {

  @Override
  public StackGresBackup mutate(BackupReview review, StackGresBackup resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    Optional.of(resource)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getBackupConfig)
        .map(StackGresBackupConfigSpec::getBaseBackups)
        .map(StackGresBaseBackupConfig::getPerformance)
        .ifPresent(performance -> {
          if (performance.getMaxDiskBandwitdh() != null) {
            if (performance.getMaxDiskBandwidth() == null) {
              performance.setMaxDiskBandwidth(performance.getMaxDiskBandwitdh());
            }
            performance.setMaxDiskBandwitdh(null);
          }
          if (performance.getMaxNetworkBandwitdh() != null) {
            if (performance.getMaxNetworkBandwidth() == null) {
              performance.setMaxNetworkBandwidth(performance.getMaxNetworkBandwitdh());
            }
            performance.setMaxNetworkBandwitdh(null);
          }
        });
    return resource;
  }

}
