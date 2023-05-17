/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.backupconfig.BaseBackupPerformance;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterBackupConfiguration {

  @JsonProperty("retention")
  private Integer retention;

  @JsonProperty("cronSchedule")
  private String cronSchedule;

  @JsonProperty("compression")
  private String compression;

  @JsonProperty("performance")
  private BaseBackupPerformance performance;

  @JsonProperty("sgObjectStorage")
  private String objectStorage;

  @JsonProperty("paths")
  private List<String> paths;

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

  public String getObjectStorage() {
    return objectStorage;
  }

  public void setObjectStorage(String objectStorage) {
    this.objectStorage = objectStorage;
  }

  public List<String> getPaths() {
    return paths;
  }

  public void setPaths(List<String> paths) {
    this.paths = paths;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
