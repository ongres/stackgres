/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedbackup;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedBackupStatus {

  private List<String> sgBackups;

  private ShardedBackupProcess process;

  private ShardedBackupInformation backupInformation;

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

  public ShardedBackupProcess getProcess() {
    return process;
  }

  public void setProcess(ShardedBackupProcess process) {
    this.process = process;
  }

  public ShardedBackupInformation getBackupInformation() {
    return backupInformation;
  }

  public void setBackupInformation(ShardedBackupInformation backupInformation) {
    this.backupInformation = backupInformation;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
