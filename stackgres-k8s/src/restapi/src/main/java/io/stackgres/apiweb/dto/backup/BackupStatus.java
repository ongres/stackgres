/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackupVolumeSnapshotStatus;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BackupStatus {

  private BackupConfigSpec sgBackupConfig;

  private String internalName;

  private String backupPath;

  private BackupProcess process;

  private BackupInformation backupInformation;

  private Boolean tested;

  private StackGresBackupVolumeSnapshotStatus volumeSnapshot;

  public BackupConfigSpec getSgBackupConfig() {
    return sgBackupConfig;
  }

  public void setSgBackupConfig(BackupConfigSpec sgBackupConfig) {
    this.sgBackupConfig = sgBackupConfig;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  public Boolean getTested() {
    return tested;
  }

  public void setTested(Boolean tested) {
    this.tested = tested;
  }

  public BackupProcess getProcess() {
    return process;
  }

  public void setProcess(BackupProcess process) {
    this.process = process;
  }

  public BackupInformation getBackupInformation() {
    return backupInformation;
  }

  public void setBackupInformation(BackupInformation backupInformation) {
    this.backupInformation = backupInformation;
  }

  public StackGresBackupVolumeSnapshotStatus getVolumeSnapshot() {
    return volumeSnapshot;
  }

  public void setVolumeSnapshot(StackGresBackupVolumeSnapshotStatus volumeSnapshot) {
    this.volumeSnapshot = volumeSnapshot;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
