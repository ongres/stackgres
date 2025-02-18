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
            StackGresShardedCluster.KIND + " "
                + getSource().getSpec().getSgShardedCluster() + " not found"));
  }

  @Value.Lazy
  default StackGresCluster getCoordinatorCluster() {
    return getFoundCoordinator()
        .orElseThrow(() -> new IllegalArgumentException(
                StackGresCluster.KIND + " "
                    + StackGresShardedClusterUtil.getCoordinatorClusterName(
                        getSource().getSpec().getSgShardedCluster())
                    + " not found for target " + StackGresShardedCluster.KIND + " "
                    + getSource().getSpec().getSgShardedCluster()));
  }

  default StackGresProfile getProfile() {
    return getFoundProfile()
        .orElseThrow(() -> new IllegalArgumentException(
            StackGresProfile.KIND + " "
                + Optional.of(getShardedCluster())
                    .map(StackGresShardedCluster::getSpec)
                    .map(StackGresShardedClusterSpec::getCoordinator)
                    .map(StackGresShardedClusterCoordinator::getSgInstanceProfile)
                    .orElse("<unknown>")
                + " not found for target " + StackGresShardedCluster.KIND + " "
                + getSource().getSpec().getSgShardedCluster()));
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

  public static class Builder extends ImmutableStackGresShardedBackupContext.Builder {
  }

  public static Builder builder() {
    return new Builder();
  }

}
