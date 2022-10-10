/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
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
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresBackupContext extends GenerationContext<StackGresBackup>, ClusterContext {

  Optional<StackGresCluster> getFoundCluster();

  Optional<StackGresProfile> getFoundProfile();

  @Override
  @Value.Derived
  default StackGresCluster getCluster() {
    return getFoundCluster()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGBackup " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target a non existent SGCluster "
                + getSource().getSpec().getSgCluster()));
  }

  @Value.Derived
  default StackGresProfile getProfile() {
    return getFoundProfile()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGBackup " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGCluster "
                + getSource().getSpec().getSgCluster()
                + " with a non existent SGInstanceProfile "
                + getFoundCluster()
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getResourceProfile)
                    .orElse("<unknown>")));
  }

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
      return HasMetadata.getFullResourceName(StackGresObjectStorage.class);
    } else {
      return HasMetadata.getFullResourceName(StackGresBackupConfig.class);
    }
  }

  default String getConfigCustomResourceName() {
    return getObjectStorage()
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getName)
        .or(() -> getBackupConfig()
            .map(HasMetadata::getMetadata)
            .map(ObjectMeta::getName)
        ).orElseThrow();
  }

  default BackupConfiguration getBackupConfiguration() {
    if (getObjectStorage().isPresent()) {
      return Optional.of(getCluster())
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getConfiguration)
          .map(StackGresClusterConfiguration::getBackups)
          .map(Collection::stream)
          .flatMap(Stream::findFirst)
          .map(bc -> new BackupConfiguration(
              bc.getRetention(),
              bc.getCronSchedule(),
              bc.getCompression(),
              bc.getPath(),
              Optional.ofNullable(bc.getPerformance())
                  .map(bp -> new BackupPerformance(
                      bp.getMaxNetworkBandwidth(),
                      bp.getMaxDiskBandwidth(),
                      bp.getUploadDiskConcurrency()))
                  .orElse(null)
          )).orElseThrow();
    } else {
      String path = getSource().getStatus().getBackupPath();
      return getBackupConfig()
          .map(StackGresBackupConfig::getSpec)
          .map(StackGresBackupConfigSpec::getBaseBackups)
          .map(bc -> new BackupConfiguration(
              bc.getRetention(),
              bc.getCronSchedule(),
              bc.getCompression(),
              path,
              Optional.ofNullable(bc.getPerformance())
                  .map(bp -> new BackupPerformance(
                      bp.getMaxNetworkBandwidth(),
                      bp.getMaxDiskBandwidth(),
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
