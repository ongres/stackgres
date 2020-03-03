/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupSpec;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupStatus;
import io.stackgres.operator.rest.dto.backup.BackupDto;
import io.stackgres.operator.rest.dto.backup.BackupSpec;
import io.stackgres.operator.rest.dto.backup.BackupStatus;

@ApplicationScoped
public class BackupTransformer extends AbstractResourceTransformer<BackupDto, StackGresBackup> {

  private final BackupConfigTransformer backupConfigTransformer;

  public BackupTransformer(BackupConfigTransformer backupConfigTransformer) {
    this.backupConfigTransformer = backupConfigTransformer;
  }

  @Override
  public StackGresBackup toCustomResource(BackupDto source) {
    StackGresBackup transformation = new StackGresBackup();
    transformation.setMetadata(getCustomResourceMetadata(source));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    transformation.setStatus(getCustomResourceStatus(source.getStatus()));
    return transformation;
  }

  @Override
  public BackupDto toResource(StackGresBackup source) {
    BackupDto transformation = new BackupDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    return transformation;
  }

  private StackGresBackupSpec getCustomResourceSpec(BackupSpec source) {
    StackGresBackupSpec transformation = new StackGresBackupSpec();
    transformation.setCluster(source.getCluster());
    transformation.setIsPermanent(source.getIsPermanent());
    return transformation;
  }

  private StackGresBackupStatus getCustomResourceStatus(BackupStatus source) {
    if (source == null) {
      return null;
    }
    StackGresBackupStatus transformation = new StackGresBackupStatus();
    transformation.setBackupConfig(
        backupConfigTransformer.getCustomResourceSpec(source.getBackupConfig()));
    transformation.setCompressedSize(source.getCompressedSize());
    transformation.setControlData(source.getControlData());
    transformation.setDataDir(source.getDataDir());
    transformation.setFailureReason(source.getFailureReason());
    transformation.setFinishLsn(source.getFinishLsn());
    transformation.setFinishTime(source.getFinishTime());
    transformation.setHostname(source.getHostname());
    transformation.setIsPermanent(source.getIsPermanent());
    transformation.setName(source.getName());
    transformation.setPgVersion(source.getPgVersion());
    transformation.setPhase(source.getPhase());
    transformation.setPod(source.getPod());
    transformation.setStartLsn(source.getStartLsn());
    transformation.setStartTime(source.getStartTime());
    transformation.setSystemIdentifier(source.getSystemIdentifier());
    transformation.setTested(source.getTested());
    transformation.setTime(source.getTime());
    transformation.setUncompressedSize(source.getUncompressedSize());
    transformation.setWalFileName(source.getWalFileName());
    return transformation;
  }

  private BackupSpec getResourceSpec(StackGresBackupSpec source) {
    BackupSpec transformation = new BackupSpec();
    transformation.setCluster(source.getCluster());
    transformation.setIsPermanent(source.getIsPermanent());
    return transformation;
  }

  private BackupStatus getResourceStatus(StackGresBackupStatus source) {
    if (source == null) {
      return null;
    }
    BackupStatus transformation = new BackupStatus();
    transformation.setBackupConfig(
        backupConfigTransformer.getResourceSpec(source.getBackupConfig()));
    transformation.setCompressedSize(source.getCompressedSize());
    transformation.setControlData(source.getControlData());
    transformation.setDataDir(source.getDataDir());
    transformation.setFailureReason(source.getFailureReason());
    transformation.setFinishLsn(source.getFinishLsn());
    transformation.setFinishTime(source.getFinishTime());
    transformation.setHostname(source.getHostname());
    transformation.setIsPermanent(source.getIsPermanent());
    transformation.setName(source.getName());
    transformation.setPgVersion(source.getPgVersion());
    transformation.setPhase(source.getPhase());
    transformation.setPod(source.getPod());
    transformation.setStartLsn(source.getStartLsn());
    transformation.setStartTime(source.getStartTime());
    transformation.setSystemIdentifier(source.getSystemIdentifier());
    transformation.setTested(source.getTested());
    transformation.setTime(source.getTime());
    transformation.setUncompressedSize(source.getUncompressedSize());
    transformation.setWalFileName(source.getWalFileName());
    return transformation;
  }

}
