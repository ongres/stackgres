/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBaseBackupPerformance;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterBackupConfiguration {

  @Positive(message = "retention must be greater than zero")
  private Integer retention;

  private String cronSchedule;

  private String compression;

  @Valid
  private StackGresBaseBackupPerformance performance;

  @NotNull
  private String sgObjectStorage;

  @NotNull
  private List<String> paths;

  private Boolean useVolumeSnapshot;

  private String volumeSnapshotClass;

  private Boolean fastVolumeSnapshot;

  private Integer timeout;

  private Integer reconciliationTimeout;

  @Min(value = 0, message = "maxRetries must be greather or equals to 0.")
  private Integer maxRetries;

  private Boolean retainWalsForUnmanagedLifecycle;

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

  public StackGresBaseBackupPerformance getPerformance() {
    return performance;
  }

  public void setPerformance(StackGresBaseBackupPerformance performance) {
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

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public Boolean getRetainWalsForUnmanagedLifecycle() {
    return retainWalsForUnmanagedLifecycle;
  }

  public void setRetainWalsForUnmanagedLifecycle(Boolean retainWalsForUnmanagedLifecycle) {
    this.retainWalsForUnmanagedLifecycle = retainWalsForUnmanagedLifecycle;
  }

  @Override
  public int hashCode() {
    return Objects.hash(compression, cronSchedule, fastVolumeSnapshot, maxRetries, paths,
        performance, reconciliationTimeout, retainWalsForUnmanagedLifecycle, retention,
        sgObjectStorage, timeout, useVolumeSnapshot, volumeSnapshotClass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterBackupConfiguration)) {
      return false;
    }
    StackGresShardedClusterBackupConfiguration other = (StackGresShardedClusterBackupConfiguration) obj;
    return Objects.equals(compression, other.compression)
        && Objects.equals(cronSchedule, other.cronSchedule)
        && Objects.equals(fastVolumeSnapshot, other.fastVolumeSnapshot)
        && Objects.equals(maxRetries, other.maxRetries) && Objects.equals(paths, other.paths)
        && Objects.equals(performance, other.performance)
        && Objects.equals(reconciliationTimeout, other.reconciliationTimeout)
        && Objects.equals(retainWalsForUnmanagedLifecycle, other.retainWalsForUnmanagedLifecycle)
        && Objects.equals(retention, other.retention)
        && Objects.equals(sgObjectStorage, other.sgObjectStorage)
        && Objects.equals(timeout, other.timeout)
        && Objects.equals(useVolumeSnapshot, other.useVolumeSnapshot)
        && Objects.equals(volumeSnapshotClass, other.volumeSnapshotClass);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
