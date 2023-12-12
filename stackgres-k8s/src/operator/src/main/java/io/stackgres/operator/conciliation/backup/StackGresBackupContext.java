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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
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
  @Value.Lazy
  default StackGresCluster getCluster() {
    return getFoundCluster()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGBackup " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target a non existent SGCluster "
                + getSource().getSpec().getSgCluster()));
  }

  @Value.Lazy
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
                    .map(StackGresClusterSpec::getSgInstanceProfile)
                    .orElse("<unknown>")));
  }

  Set<String> getClusterBackupNamespaces();

  Optional<StackGresObjectStorage> getObjectStorage();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  default String getConfigCrdName() {
    return HasMetadata.getFullResourceName(StackGresObjectStorage.class);
  }

  default String getConfigCustomResourceName() {
    return getObjectStorage()
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getName)
        .orElseThrow();
  }

  default BackupConfiguration getBackupConfiguration() {
    return Optional.of(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBackups)
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
                bp.getUploadDiskConcurrency(),
                bp.getUploadConcurrency(),
                bp.getDownloadConcurrency()))
            .orElse(null),
            Optional.ofNullable(bc.getUseVolumeSnapshot())
            .orElse(false),
            bc.getVolumeSnapshotClass(),
            bc.getFastVolumeSnapshot()))
        .orElseThrow();
  }

  default BackupStorage getBackupStorage() {
    return getObjectStorage().map(CustomResource::getSpec)
        .orElseThrow();
  }

}
