/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.backupconfig.BaseBackupPerformance;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterBackupsConfiguration {

  @JsonProperty("retention")
  @Positive(message = "retention should be greater than zero")
  private Integer retention;

  @JsonProperty("cronSchedule")
  private String cronSchedule;

  @JsonProperty("compression")
  private String compressionMethod;

  @Valid
  private BaseBackupPerformance performance;

  @JsonProperty("sgObjectStorage")
  @NotNull
  private String objectStorage;

  @JsonProperty("path")
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

  public String getCompressionMethod() {
    return compressionMethod;
  }

  public void setCompressionMethod(String compressionMethod) {
    this.compressionMethod = compressionMethod;
  }

  public BaseBackupPerformance getPerformance() {
    return performance;
  }

  public void setPerformance(BaseBackupPerformance performance) {
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
    if (!(obj instanceof ClusterBackupsConfiguration)) {
      return false;
    }
    ClusterBackupsConfiguration other = (ClusterBackupsConfiguration) obj;
    return Objects.equals(compressionMethod, other.compressionMethod)
        && Objects.equals(cronSchedule, other.cronSchedule)
        && Objects.equals(objectStorage, other.objectStorage) && Objects.equals(path, other.path)
        && Objects.equals(performance, other.performance)
        && Objects.equals(retention, other.retention);
  }

  @Override
  public int hashCode() {
    return Objects.hash(compressionMethod, cronSchedule, objectStorage, path, performance,
        retention);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
