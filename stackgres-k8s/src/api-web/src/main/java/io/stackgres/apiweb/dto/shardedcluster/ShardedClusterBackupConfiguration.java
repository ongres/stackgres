/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.backup.BaseBackupPerformance;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterBackupConfiguration {

  private Integer retention;

  private String cronSchedule;

  private String compression;

  private BaseBackupPerformance performance;

  private String sgObjectStorage;

  private List<String> paths;

  private Boolean useVolumeSnapshot;

  private String volumeSnapshotClass;

  private Boolean fastVolumeSnapshot;

  private Integer timeout;

  private Integer reconciliationTimeout;

  public Integer getRetention() {
    return retention;
  }

  public void setRetention(Integer retention) {
    this.retention = retention;
  }

  public String getCronSchedule() {
    return cronSchedule;
  }

  public void setCronSchedule(String cronSchedule) {
    this.cronSchedule = cronSchedule;
  }

  public String getCompression() {
    return compression;
  }

  public void setCompression(String compression) {
    this.compression = compression;
  }

  public BaseBackupPerformance getPerformance() {
    return performance;
  }

  public void setPerformance(BaseBackupPerformance performance) {
    this.performance = performance;
  }

  public String getSgObjectStorage() {
    return sgObjectStorage;
  }

  public void setSgObjectStorage(String sgObjectStorage) {
    this.sgObjectStorage = sgObjectStorage;
  }

  public List<String> getPaths() {
    return paths;
  }

  public void setPaths(List<String> paths) {
    this.paths = paths;
  }

  public Boolean getUseVolumeSnapshot() {
    return useVolumeSnapshot;
  }

  public void setUseVolumeSnapshot(Boolean useVolumeSnapshot) {
    this.useVolumeSnapshot = useVolumeSnapshot;
  }

  public String getVolumeSnapshotClass() {
    return volumeSnapshotClass;
  }

  public void setVolumeSnapshotClass(String volumeSnapshotClass) {
    this.volumeSnapshotClass = volumeSnapshotClass;
  }

  public Boolean getFastVolumeSnapshot() {
    return fastVolumeSnapshot;
  }

  public void setFastVolumeSnapshot(Boolean fastVolumeSnapshot) {
    this.fastVolumeSnapshot = fastVolumeSnapshot;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public Integer getReconciliationTimeout() {
    return reconciliationTimeout;
  }

  public void setReconciliationTimeout(Integer reconciliationTimeout) {
    this.reconciliationTimeout = reconciliationTimeout;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
