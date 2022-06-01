/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterManagedScriptEntryStatus {

  @JsonProperty("id")
  @NotNull(message = "id can not be null")
  private Integer id;

  @JsonProperty("startedAt")
  private String startedAt;

  @JsonProperty("failedAt")
  private String failedAt;

  @JsonProperty("completedAt")
  private String completedAt;

  @JsonProperty("scripts")
  @Valid
  private List<StackGresClusterManagedScriptEntryScriptsStatus> scripts;

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

  public List<StackGresClusterManagedScriptEntryScriptsStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresClusterManagedScriptEntryScriptsStatus> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(completedAt, failedAt, id, scripts, startedAt);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterManagedScriptEntryStatus)) {
      return false;
    }
    StackGresClusterManagedScriptEntryStatus other = (StackGresClusterManagedScriptEntryStatus) obj;
    return Objects.equals(completedAt, other.completedAt)
        && Objects.equals(failedAt, other.failedAt) && Objects.equals(id, other.id)
        && Objects.equals(scripts, other.scripts) && Objects.equals(startedAt, other.startedAt);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
