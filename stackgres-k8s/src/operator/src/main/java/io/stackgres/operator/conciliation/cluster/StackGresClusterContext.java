/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.GenerationContext;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import io.stackgres.operator.conciliation.factory.PatroniScriptsConfigMap;
import org.immutables.value.Value;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple4;

@Value.Immutable
public interface StackGresClusterContext extends GenerationContext<StackGresCluster>,
    ClusterContext {

  @Override
  @Value.Derived
  default StackGresCluster getCluster() {
    return getSource();
  }

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  Optional<StackGresBackupConfig> getBackupConfig();

  Optional<StackGresObjectStorage> getObjectStorageConfig();

  StackGresPostgresConfig getPostgresConfig();

  StackGresProfile getStackGresProfile();

  Optional<StackGresPoolingConfig> getPoolingConfig();

  Optional<StackGresBackup> getRestoreBackup();

  List<StackGresClusterScriptEntry> getInternalScripts();

  Optional<Prometheus> getPrometheus();

  Optional<Secret> getDatabaseCredentials();

  Set<String> getClusterBackupNamespaces();

  @Value.Derived
  default List<Tuple4<StackGresClusterScriptEntry, Long, String, Long>> getIndexedScripts() {
    Seq<StackGresClusterScriptEntry> internalScripts = Seq.seq(getInternalScripts());
    return internalScripts
        .zipWithIndex()
        .map(t -> t.concat(PatroniScriptsConfigMap.INTERNAL_SCRIPT))
        .append(Seq.of(Optional.ofNullable(
                    getSource().getSpec().getInitData())
                .map(StackGresClusterInitData::getScripts))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .zipWithIndex()
            .map(t -> t.concat(PatroniScriptsConfigMap.SCRIPT)))
        .zipWithIndex()
        .map(t -> t.v1.concat(t.v2))
        .toList();
  }

  default Optional<BackupConfiguration> getBackupConfiguration() {
    if (getObjectStorageConfig().isPresent()) {
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
              bc.getPath(),
              Optional.ofNullable(bc.getPerformance())
                  .map(bp -> new BackupPerformance(
                      bp.getMaxNetworkBandwitdh(),
                      bp.getMaxDiskBandwitdh(),
                      bp.getUploadDiskConcurrency()))
                  .orElse(null)
          ));
    } else {
      String path = Optional.of(getCluster())
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getConfiguration)
          .map(StackGresClusterConfiguration::getBackupPath)
          .orElseThrow();
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
                      bp.getMaxNetworkBandwitdh(),
                      bp.getMaxDiskBandwitdh(),
                      bp.getUploadDiskConcurrency()
                  )).orElse(null)
          ));
    }
  }

  default Optional<BackupStorage> getBackupStorage() {
    return getObjectStorageConfig().map(CustomResource::getSpec)
        .or(() -> getBackupConfig().map(StackGresBackupConfig::getSpec)
            .map(StackGresBackupConfigSpec::getStorage));
  }

  default String getConfigCrdName() {
    if (getObjectStorageConfig().isPresent()) {
      return CustomResource.getCRDName(StackGresObjectStorage.class);
    } else {
      return CustomResource.getCRDName(StackGresBackupConfig.class);
    }
  }

  default Optional<ObjectMeta> getBackupConfigurationMetadata() {
    return getObjectStorageConfig()
        .map(CustomResource::getMetadata)
        .or(() -> getBackupConfig().map(CustomResource::getMetadata));
  }

  default Optional<String> getBackupConfigurationCustomResourceName() {
    return getBackupConfigurationMetadata()
        .map(ObjectMeta::getName);
  }

  default Optional<String> getBackupConfigurationResourceVersion() {
    return getBackupConfigurationMetadata()
        .map(ObjectMeta::getResourceVersion);
  }

}
