/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBaseBackupPerformance;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterBackupConfiguration extends AdditionalProperties {

  @Positive(message = "retention must be greater than zero")
  private Integer retention;

  private String cronSchedule;

  private String compression;

  @Valid
  private StackGresBaseBackupPerformance performance;

  @NotNull
  private String sgObjectStorage;

  private String path;

  private Boolean useVolumeSnapshot;

  private String volumeSnapshotClass;

  private Boolean fastVolumeSnapshot;

  private Integer timeout;

  private Integer reconciliationTimeout;

  @Min(value = 0, message = "maxRetries must be greather or equals to 0.")
  private Integer maxRetries;

  private String retryDelay;

  @Min(value = 0, message = "retryLimit must be greather or equals to 0.")
  private Integer retryLimit;

  private String retryMaxDelay;

  private Boolean retainWalsForUnmanagedLifecycle;

  @ReferencedField("retryDelay")
  interface RetryDelay extends FieldReference {
  }

  @ReferencedField("retryMaxDelay")
  interface RetryMaxDelay extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "retryDelay must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = RetryDelay.class)
  public boolean isRetryDelayValid() {
    try {
      if (retryDelay != null) {
        return !Duration.parse(retryDelay).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  @JsonIgnore
  @AssertTrue(message = "retryMaxDelay must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = RetryMaxDelay.class)
  public boolean isRetryMaxDelayValid() {
    try {
      if (retryMaxDelay != null) {
        return !Duration.parse(retryMaxDelay).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

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

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
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

  public String getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(String retryDelay) {
    this.retryDelay = retryDelay;
  }

  public Integer getRetryLimit() {
    return retryLimit;
  }

  public void setRetryLimit(Integer retryLimit) {
    this.retryLimit = retryLimit;
  }

  public String getRetryMaxDelay() {
    return retryMaxDelay;
  }

  public void setRetryMaxDelay(String retryMaxDelay) {
    this.retryMaxDelay = retryMaxDelay;
  }

  public Boolean getRetainWalsForUnmanagedLifecycle() {
    return retainWalsForUnmanagedLifecycle;
  }

  public void setRetainWalsForUnmanagedLifecycle(Boolean retainWalsForUnmanagedLifecycle) {
    this.retainWalsForUnmanagedLifecycle = retainWalsForUnmanagedLifecycle;
  }

  @Override
  public int hashCode() {
    return Objects.hash(compression, cronSchedule, fastVolumeSnapshot, maxRetries, path,
        performance, reconciliationTimeout, retainWalsForUnmanagedLifecycle, retention,
        retryDelay, retryLimit, retryMaxDelay, sgObjectStorage, timeout, useVolumeSnapshot,
        volumeSnapshotClass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterBackupConfiguration)) {
      return false;
    }
    StackGresClusterBackupConfiguration other = (StackGresClusterBackupConfiguration) obj;
    return Objects.equals(compression, other.compression)
        && Objects.equals(cronSchedule, other.cronSchedule)
        && Objects.equals(fastVolumeSnapshot, other.fastVolumeSnapshot)
        && Objects.equals(maxRetries, other.maxRetries) && Objects.equals(path, other.path)
        && Objects.equals(performance, other.performance)
        && Objects.equals(reconciliationTimeout, other.reconciliationTimeout)
        && Objects.equals(retainWalsForUnmanagedLifecycle, other.retainWalsForUnmanagedLifecycle)
        && Objects.equals(retention, other.retention)
        && Objects.equals(retryDelay, other.retryDelay)
        && Objects.equals(retryLimit, other.retryLimit)
        && Objects.equals(retryMaxDelay, other.retryMaxDelay)
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
