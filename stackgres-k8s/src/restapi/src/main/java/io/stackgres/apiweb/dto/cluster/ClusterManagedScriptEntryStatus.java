/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterManagedScriptEntryStatus {

  private Integer id;

  private String startedAt;

  private String updatedAt;

  private String failedAt;

  private String completedAt;

  private List<ClusterManagedScriptEntryScriptsStatus> scripts;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getFailedAt() {
    return failedAt;
  }

  public void setFailedAt(String failedAt) {
    this.failedAt = failedAt;
  }

  public String getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(String completedAt) {
    this.completedAt = completedAt;
  }

  public List<ClusterManagedScriptEntryScriptsStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<ClusterManagedScriptEntryScriptsStatus> scripts) {
    this.scripts = scripts;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
