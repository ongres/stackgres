/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBaseBackupConfig {

  @JsonProperty("retention")
  @Positive(message = "retention should be greater than zero")
  private int retention;

  @JsonProperty("cronSchedule")
  private String cronSchedule;

  @JsonProperty("compression")
  private String compression;

  @JsonProperty("performance")
  @Valid
  private StackGresBaseBackupPerformance performance;

  public int getRetention() {
    return retention;
  }

  public void setRetention(int retention) {
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("retention", retention)
        .add("cronSchedule", cronSchedule)
        .add("compression", compression)
        .add("performance", performance)
        .toString();
  }
}
