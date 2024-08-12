/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BackupVolumeSnapshotStatus {

  private String name;

  private String backupLabel;

  private String tablespaceMap;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBackupLabel() {
    return backupLabel;
  }

  public void setBackupLabel(String backupLabel) {
    this.backupLabel = backupLabel;
  }

  public String getTablespaceMap() {
    return tablespaceMap;
  }

  public void setTablespaceMap(String tablespaceMap) {
    this.tablespaceMap = tablespaceMap;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
