/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backupconfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BaseBackupConfig {

  @JsonProperty("retention")
  private Integer retention;

  @JsonProperty("cronSchedule")
  private String cronSchedule;

  @JsonProperty("compression")
  private String compressionMethod;

  private BaseBackupPerformance performance;

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

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
