/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBackupVolumeSnapshotStatus {

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
  public int hashCode() {
    return Objects.hash(backupLabel, name, tablespaceMap);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupVolumeSnapshotStatus)) {
      return false;
    }
    StackGresBackupVolumeSnapshotStatus other = (StackGresBackupVolumeSnapshotStatus) obj;
    return Objects.equals(backupLabel, other.backupLabel) && Objects.equals(name, other.name)
        && Objects.equals(tablespaceMap, other.tablespaceMap);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
