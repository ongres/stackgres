/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigSpec;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BackupStatus {

  @JsonProperty("sgBackupConfig")
  private BackupConfigSpec backupConfig;

  private String internalName;

  private String backupPath;

  private BackupProcess process;

  private BackupInformation backupInformation;

  private Boolean tested;

  public BackupConfigSpec getBackupConfig() {
    return backupConfig;
  }

  public void setBackupConfig(BackupConfigSpec backupConfig) {
    this.backupConfig = backupConfig;
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

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
