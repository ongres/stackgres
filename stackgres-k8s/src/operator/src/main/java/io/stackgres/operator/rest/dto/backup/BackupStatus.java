/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigSpec;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupStatus {

  @JsonProperty("sgBackupConfig")
  private BackupConfigSpec backupConfig;

  private String internalName;

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
    return MoreObjects.toStringHelper(this)
        .add("backupConfig", backupConfig)
        .add("internalName", internalName)
        .add("process", process)
        .add("backupInformation", backupInformation)
        .add("tested", tested)
        .toString();
  }
}
