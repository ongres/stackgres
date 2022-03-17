/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresBackupContext extends GenerationContext<StackGresBackup>, ClusterContext {

  Optional<StackGresBackupConfig> getBackupConfig();

  Set<String> getClusterBackupNamespaces();

  Optional<StackGresObjectStorage> getObjectStorage();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  default String getConfigCrdName() {
    if (getObjectStorage().isPresent()) {
      return CustomResource.getCRDName(StackGresObjectStorage.class);
    } else {
      return CustomResource.getCRDName(StackGresBackupConfig.class);
    }
  }

  default String getConfigCustomResourceName() {
    return getObjectStorage()
        .map(CustomResource::getMetadata)
        .map(ObjectMeta::getName)
        .or(() -> getBackupConfig()
            .map(CustomResource::getMetadata)
            .map(ObjectMeta::getName)
        ).orElseThrow();
  }

  default BackupConfiguration getBackupConfiguration() {
    if (getObjectStorage().isPresent()) {
      return Optional.of(getCluster())
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getConfiguration)
          .map(StackGresClusterConfiguration::getBackups)
          .filter(bs -> !bs.isEmpty())
          .map(bs -> bs.get(0))
          .map(bc -> new BackupConfiguration(
              bc.getRetention(),
              bc.getCronSchedule(),
              bc.getCompression(),
              Optional.ofNullable(bc.getPerformance())
                  .map(bp -> new BackupPerformance(
                      bp.getMaxNetworkBandwitdh(),
                      bp.getMaxDiskBandwitdh(),
                      bp.getUploadDiskConcurrency()))
                  .orElse(null)
          )).orElseThrow();
    } else {
      return getBackupConfig()
          .map(StackGresBackupConfig::getSpec)
          .map(StackGresBackupConfigSpec::getBaseBackups)
          .map(bc -> new BackupConfiguration(
              bc.getRetention(),
              bc.getCronSchedule(),
              bc.getCompression(),
              Optional.ofNullable(bc.getPerformance())
                  .map(bp -> new BackupPerformance(
                      bp.getMaxNetworkBandwitdh(),
                      bp.getMaxDiskBandwitdh(),
                      bp.getUploadDiskConcurrency()
                  )).orElse(null)
          )).orElseThrow();
    }
  }

  default BackupStorage getBackupStorage() {
    return getObjectStorage().map(CustomResource::getSpec)
        .or(() -> getBackupConfig().map(StackGresBackupConfig::getSpec)
            .map(StackGresBackupConfigSpec::getStorage))
        .orElseThrow();
  }

}
