/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigSpec;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigStatus;
import io.stackgres.apiweb.dto.backupconfig.BaseBackupConfig;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.storages.BackupStorage;

@ApplicationScoped
public class BackupConfigTransformer
    extends AbstractDependencyResourceTransformer<BackupConfigDto, StackGresBackupConfig> {

  private final Transformer<BackupStorageDto, BackupStorage> storageTransformer;
  private final ObjectMapper objectMapper;

  @Inject
  public BackupConfigTransformer(
      Transformer<BackupStorageDto, BackupStorage> storageTransformer,
      ObjectMapper objectMapper) {
    this.storageTransformer = storageTransformer;
    this.objectMapper = objectMapper;
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
        .ifPresent(sourceBaseBackup -> transformation.setBaseBackups(
            objectMapper.convertValue(sourceBaseBackup, StackGresBaseBackupConfig.class)
        ));

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
        .ifPresent(sourceBaseBackup -> transformation.setBaseBackup(
            objectMapper.convertValue(sourceBaseBackup,
                BaseBackupConfig.class)
        ));

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
