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
import org.jooq.lambda.Seq;

@Value.Immutable
public interface StackGresBackupContext extends GenerationContext<StackGresBackup>, ClusterContext {

  Optional<StackGresCluster> getFoundCluster();

  Optional<StackGresProfile> getFoundProfile();

  @Override
  @Value.Lazy
  default StackGresCluster getCluster() {
    return getFoundCluster()
        .orElseThrow(() -> new IllegalArgumentException(
            StackGresCluster.KIND + " " + getSource().getSpec().getSgCluster() + " not found"));
  }

  @Value.Lazy
  default StackGresProfile getProfile() {
    return getFoundProfile()
        .orElseThrow(() -> new IllegalArgumentException(
            StackGresProfile.KIND + " " + getCluster().getSpec().getSgInstanceProfile() + " not found"));
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
        .map(Seq::seq)
        .map(seq -> seq.zipWithIndex())
        .flatMap(Stream::findFirst)
        .map(bc -> new BackupConfiguration(
            bc.v1.getRetention(),
            bc.v1.getCronSchedule(),
            bc.v1.getCompression(),
            getCluster().getStatus().getBackupPaths().get(bc.v2.intValue()),
            Optional.ofNullable(bc.v1.getPerformance())
            .map(bp -> new BackupPerformance(
                bp.getMaxNetworkBandwidth(),
                bp.getMaxDiskBandwidth(),
                bp.getUploadDiskConcurrency(),
                bp.getUploadConcurrency(),
                bp.getDownloadConcurrency()))
            .orElse(null),
            Optional.ofNullable(bc.v1.getUseVolumeSnapshot())
            .orElse(false),
            bc.v1.getVolumeSnapshotClass(),
            bc.v1.getFastVolumeSnapshot(),
            bc.v1.getTimeout(),
            bc.v1.getReconciliationTimeout(),
            bc.v1.getMaxRetries(),
            bc.v1.getRetainWalsForUnmanagedLifecycle()))
        .orElseThrow();
  }

  default BackupStorage getBackupStorage() {
    return getObjectStorage().map(CustomResource::getSpec)
        .orElseThrow();
  }

  public static class Builder extends ImmutableStackGresBackupContext.Builder {
  }

  public static Builder builder() {
    return new Builder();
  }

}
