/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupStatus implements KubernetesResource {

  private static final long serialVersionUID = 4124027524757318245L;

  @JsonProperty("sgBackupConfig")
  private StackGresBackupConfigSpec backupConfig;

  private String internalName;

  private StackGresBackupProcess process;

  private StackGresBackupInformation backupInformation;

  private Boolean tested;

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public StackGresBackupConfigSpec getBackupConfig() {
    return backupConfig;
  }

  public void setBackupConfig(StackGresBackupConfigSpec backupConfig) {
    this.backupConfig = backupConfig;
  }

  public Boolean getTested() {
    return tested;
  }

  public void setTested(Boolean tested) {
    this.tested = tested;
  }

  public StackGresBackupProcess getProcess() {
    return process;
  }

  public void setProcess(StackGresBackupProcess process) {
    this.process = process;
  }

  public StackGresBackupInformation getBackupInformation() {
    return backupInformation;
  }

  public void setBackupInformation(StackGresBackupInformation backupInformation) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresBackupStatus that = (StackGresBackupStatus) o;
    return Objects.equals(backupConfig, that.backupConfig)
        && Objects.equals(internalName, that.internalName) && Objects.equals(process, that.process)
        && Objects.equals(backupInformation, that.backupInformation)
        && Objects.equals(tested, that.tested);
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupConfig, internalName, process, backupInformation, tested);
  }
}
