/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterManagedScriptEntryStatus {

  @NotNull(message = "id can not be null")
  private Integer id;

  private String startedAt;

  private String updatedAt;

  private String failedAt;

  private String completedAt;

  @Valid
  private List<StackGresClusterManagedScriptEntryScriptStatus> scripts;

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

  public List<StackGresClusterManagedScriptEntryScriptStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresClusterManagedScriptEntryScriptStatus> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(completedAt, failedAt, id, scripts, startedAt, updatedAt);
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
        && Objects.equals(scripts, other.scripts) && Objects.equals(startedAt, other.startedAt)
        && Objects.equals(updatedAt, other.updatedAt);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
