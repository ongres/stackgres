/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.backupconfig.BaseBackupPerformance;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterBackupsConfiguration {

  private Integer retention;

  private String cronSchedule;

  private String compression;

  private BaseBackupPerformance performance;

  private String sgObjectStorage;

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

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
