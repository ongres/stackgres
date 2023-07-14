/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBackupStatus {

  @JsonProperty("sgBackupConfig")
  @Valid
  private StackGresBackupConfigSpec backupConfig;

  @JsonProperty("internalName")
  private String internalName;

  @JsonProperty("backupPath")
  private String backupPath;

  @JsonProperty("process")
  @Valid
  private StackGresBackupProcess process;

  @JsonProperty("backupInformation")
  @Valid
  private StackGresBackupInformation backupInformation;

  @JsonProperty("tested")
  private Boolean tested;

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
  public int hashCode() {
    return Objects.hash(backupConfig, backupInformation, backupPath, internalName, process, tested);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupStatus)) {
      return false;
    }
    StackGresBackupStatus other = (StackGresBackupStatus) obj;
    return Objects.equals(backupConfig, other.backupConfig)
        && Objects.equals(backupInformation, other.backupInformation)
        && Objects.equals(backupPath, other.backupPath)
        && Objects.equals(internalName, other.internalName)
        && Objects.equals(process, other.process) && Objects.equals(tested, other.tested);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
