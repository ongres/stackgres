/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedbackup;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedBackupStatus {

  private List<String> sgBackups;

  @Valid
  private StackGresShardedBackupProcess process;

  @Valid
  private StackGresShardedBackupInformation backupInformation;

  private Boolean tested;

  public List<String> getSgBackups() {
    return sgBackups;
  }

  public void setSgBackups(List<String> sgBackups) {
    this.sgBackups = sgBackups;
  }

  public Boolean getTested() {
    return tested;
  }

  public void setTested(Boolean tested) {
    this.tested = tested;
  }

  public StackGresShardedBackupProcess getProcess() {
    return process;
  }

  public void setProcess(StackGresShardedBackupProcess process) {
    this.process = process;
  }

  public StackGresShardedBackupInformation getBackupInformation() {
    return backupInformation;
  }

  public void setBackupInformation(StackGresShardedBackupInformation backupInformation) {
    this.backupInformation = backupInformation;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupInformation, process, sgBackups, tested);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedBackupStatus)) {
      return false;
    }
    StackGresShardedBackupStatus other = (StackGresShardedBackupStatus) obj;
    return Objects.equals(backupInformation, other.backupInformation)
        && Objects.equals(process, other.process)
        && Objects.equals(sgBackups, other.sgBackups)
        && Objects.equals(tested, other.tested);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
