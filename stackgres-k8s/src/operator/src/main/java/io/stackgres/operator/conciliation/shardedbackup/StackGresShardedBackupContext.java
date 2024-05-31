/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.stackgres.common.ShardedClusterContext;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresShardedBackupContext
    extends GenerationContext<StackGresShardedBackup>, ShardedClusterContext {

  Optional<StackGresShardedCluster> getFoundShardedCluster();

  Optional<StackGresCluster> getFoundCoordinator();

  Optional<StackGresProfile> getFoundProfile();

  @Value.Lazy
  @Override
  default StackGresShardedCluster getShardedCluster() {
    return getFoundShardedCluster()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGShardedBackup " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target a non existent SGShardedCluster "
                + getSource().getSpec().getSgShardedCluster()));
  }

  @Value.Lazy
  default StackGresCluster getCoordinatorCluster() {
    return getFoundCoordinator()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGShardedBackup " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGShardedCluster "
                + getSource().getSpec().getSgShardedCluster()
                + " with a non existent coordinator SGCluster "
                + StackGresShardedClusterUtil.getCoordinatorClusterName(
                    getSource().getSpec().getSgShardedCluster())));
  }

  default StackGresProfile getProfile() {
    return getFoundProfile()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGShardedBackup " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGShardedCluster "
                + getSource().getSpec().getSgShardedCluster()
                + " with a non existent SGInstanceProfile "
                + getFoundShardedCluster()
                    .map(StackGresShardedCluster::getSpec)
                    .map(StackGresShardedClusterSpec::getCoordinator)
                    .map(StackGresShardedClusterCoordinator::getSgInstanceProfile)
                    .orElse("<unknown>")));
  }

  Set<String> getClusterBackupNamespaces();

  Optional<StackGresObjectStorage> getObjectStorage();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  default ShardedBackupConfiguration getBackupConfiguration() {
    return Optional.of(getShardedCluster())
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getBackups)
        .map(Collection::stream)
        .flatMap(Stream::findFirst)
        .map(bc -> new ShardedBackupConfiguration(
            bc.getRetention(),
            bc.getCronSchedule(),
            bc.getCompression(),
            bc.getPaths(),
            Optional.ofNullable(bc.getPerformance())
            .map(bp -> new ShardedBackupPerformance(
                bp.getMaxNetworkBandwidth(),
                bp.getMaxDiskBandwidth(),
                bp.getUploadDiskConcurrency(),
                bp.getUploadConcurrency(),
                bp.getDownloadConcurrency()))
            .orElse(null),
            Optional.ofNullable(bc.getUseVolumeSnapshot())
            .orElse(false),
            bc.getVolumeSnapshotClass(),
            bc.getFastVolumeSnapshot(),
            bc.getTimeout(),
            bc.getReconciliationTimeout(),
            bc.getMaxRetries(),
            bc.getRetainWalsForUnmanagedLifecycle()))
        .orElseThrow();
  }

}
