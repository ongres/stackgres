/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigSpec;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigStatus;
import io.stackgres.apiweb.dto.backupconfig.BaseBackupConfig;
import io.stackgres.apiweb.dto.backupconfig.BaseBackupPerformance;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;
import io.stackgres.common.crd.storages.BackupStorage;

@ApplicationScoped
public class BackupConfigTransformer
    extends AbstractDependencyResourceTransformer<BackupConfigDto, StackGresBackupConfig> {

  private final Transformer<BackupStorageDto, BackupStorage> storageTransformer;

  @Inject
  public BackupConfigTransformer(Transformer<BackupStorageDto, BackupStorage> storageTransformer) {
    this.storageTransformer = storageTransformer;
  }

  @Override
  public StackGresBackupConfig toCustomResource(BackupConfigDto source,
                                                StackGresBackupConfig original) {
    StackGresBackupConfig transformation = Optional.ofNullable(original)
        .orElseGet(StackGresBackupConfig::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public BackupConfigDto toResource(StackGresBackupConfig source, List<String> clusters) {
    BackupConfigDto transformation = new BackupConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(clusters));
    return transformation;
  }

  public StackGresBackupConfigSpec getCustomResourceSpec(BackupConfigSpec source) {
    if (source == null) {
      return null;
    }
    StackGresBackupConfigSpec transformation = new StackGresBackupConfigSpec();
    Optional.ofNullable(source.getBaseBackups())
        .ifPresent(sourceBaseBackup -> {
          final StackGresBaseBackupConfig baseBackup = new StackGresBaseBackupConfig();
          transformation.setBaseBackups(baseBackup);
          baseBackup.setCompression(source.getBaseBackups().getCompressionMethod());
          baseBackup.setCronSchedule(sourceBaseBackup.getCronSchedule());
          baseBackup.setRetention(sourceBaseBackup.getRetention());
        });

    Optional.ofNullable(source.getBaseBackups())
        .map(BaseBackupConfig::getPerformance)
        .ifPresent(sourcePerformance -> {
          final StackGresBaseBackupPerformance performance = new StackGresBaseBackupPerformance();
          transformation.getBaseBackups().setPerformance(performance);
          performance.setMaxDiskBandwitdh(sourcePerformance.getMaxDiskBandwitdh());
          performance.setMaxNetworkBandwitdh(sourcePerformance.getMaxNetworkBandwitdh());
          performance.setUploadDiskConcurrency(sourcePerformance.getUploadDiskConcurrency());
        });

    transformation.setStorage(getCustomResourceStorage(source.getStorage()));
    return transformation;
  }

  private io.stackgres.common.crd.storages.BackupStorage getCustomResourceStorage(
      BackupStorageDto source
  ) {
    return storageTransformer.toSource(source);
  }

  public BackupConfigSpec getResourceSpec(StackGresBackupConfigSpec source) {
    if (source == null) {
      return null;
    }
    BackupConfigSpec transformation = new BackupConfigSpec();
    Optional.ofNullable(source.getBaseBackups())
        .ifPresent(sourceBaseBackup -> {
          final BaseBackupConfig baseBackup = new BaseBackupConfig();
          baseBackup.setCompressionMethod(sourceBaseBackup.getCompression());
          baseBackup.setCronSchedule(sourceBaseBackup.getCronSchedule());
          baseBackup.setRetention(sourceBaseBackup.getRetention());
          transformation.setBaseBackup(baseBackup);
        });

    Optional.ofNullable(source.getBaseBackups())
        .map(StackGresBaseBackupConfig::getPerformance)
        .ifPresent(sourcePerformance -> {
          final BaseBackupPerformance performance = new BaseBackupPerformance();
          performance.setMaxDiskBandwitdh(sourcePerformance.getMaxDiskBandwitdh());
          performance.setMaxNetworkBandwitdh(sourcePerformance.getMaxNetworkBandwitdh());
          performance.setUploadDiskConcurrency(sourcePerformance.getUploadDiskConcurrency());

          transformation.getBaseBackups().setPerformance(performance);
        });

    transformation.setStorage(getResourceStorage(source.getStorage()));
    return transformation;
  }

  public BackupConfigStatus getResourceStatus(List<String> clusters) {
    BackupConfigStatus transformation = new BackupConfigStatus();
    transformation.setClusters(clusters);
    return transformation;
  }

  private BackupStorageDto getResourceStorage(
      io.stackgres.common.crd.storages.BackupStorage source) {
    if (source == null) {
      return null;
    }
    return storageTransformer.toTarget(source);
  }
}
