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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterBackupsConfiguration that = (ClusterBackupsConfiguration) o;
    return Objects.equals(retention, that.retention)
        && Objects.equals(cronSchedule, that.cronSchedule)
        && Objects.equals(compressionMethod, that.compressionMethod)
        && Objects.equals(performance, that.performance)
        && Objects.equals(objectStorage, that.objectStorage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(retention, cronSchedule, compressionMethod, performance, objectStorage);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
