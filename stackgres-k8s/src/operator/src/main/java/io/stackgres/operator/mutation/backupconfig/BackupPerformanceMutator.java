/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import java.util.Optional;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupPerformanceMutator implements BackupConfigMutator {

  @Override
  public StackGresBackupConfig mutate(BackupConfigReview review, StackGresBackupConfig resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    Optional.of(resource.getSpec())
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
