/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.backup.BaseBackupPerformance;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterReplicationInitialization {

  private String mode;

  private String backupNewerThan;

  private BaseBackupPerformance backupRestorePerformance;

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getBackupNewerThan() {
    return backupNewerThan;
  }

  public void setBackupNewerThan(String backupNewerThan) {
    this.backupNewerThan = backupNewerThan;
  }

  public BaseBackupPerformance getBackupRestorePerformance() {
    return backupRestorePerformance;
  }

  public void setBackupRestorePerformance(BaseBackupPerformance backupRestorePerformance) {
    this.backupRestorePerformance = backupRestorePerformance;
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
