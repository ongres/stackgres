/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterBackupConfiguration {

  @JsonProperty("retention")
  @Positive(message = "retention should be greater than zero")
  private Integer retention;

  @JsonProperty("cronSchedule")
  private String cronSchedule;

  @JsonProperty("compression")
  private String compression;

  @JsonProperty("performance")
  @Valid
  private StackGresBaseBackupPerformance performance;

  @JsonProperty("sgObjectStorage")
  @NotNull
  private String objectStorage;

  @JsonProperty("path")
  @NotNull
  private String path;

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

  public String getObjectStorage() {
    return objectStorage;
  }

  public void setObjectStorage(String objectStorage) {
    this.objectStorage = objectStorage;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
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
    return Objects.equals(path, other.path)
        && Objects.equals(compression, other.compression)
        && Objects.equals(cronSchedule, other.cronSchedule)
        && Objects.equals(objectStorage, other.objectStorage)
        && Objects.equals(performance, other.performance)
        && Objects.equals(retention, other.retention);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, compression, cronSchedule, objectStorage, performance,
        retention);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
