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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresClusterBackupConfiguration that = (StackGresClusterBackupConfiguration) o;
    return Objects.equals(retention, that.retention)
        && Objects.equals(cronSchedule, that.cronSchedule)
        && Objects.equals(compression, that.compression)
        && Objects.equals(performance, that.performance)
        && Objects.equals(objectStorage, that.objectStorage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(retention, cronSchedule, compression, performance, objectStorage);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
