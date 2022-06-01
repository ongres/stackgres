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
public class StackGresClusterManagedSqlStatus {

  @JsonProperty("lastId")
  @NotNull(message = "lastId can not be null")
  private Integer lastId;

  @JsonProperty("scripts")
  @Valid
  private List<StackGresClusterManagedScriptEntryStatus> scripts;

  public Integer getLastId() {
    return lastId;
  }

  public void setLastId(Integer lastId) {
    this.lastId = lastId;
  }

  public List<StackGresClusterManagedScriptEntryStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresClusterManagedScriptEntryStatus> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastId, scripts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterManagedSqlStatus)) {
      return false;
    }
    StackGresClusterManagedSqlStatus other = (StackGresClusterManagedSqlStatus) obj;
    return Objects.equals(lastId, other.lastId) && Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
