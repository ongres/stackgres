/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterManagedSqlStatus {

  @JsonProperty("lastId")
  private Integer lastId;

  @JsonProperty("scripts")
  private List<ClusterManagedScriptEntryStatus> scripts;

  public Integer getLastId() {
    return lastId;
  }

  public void setLastId(Integer lastId) {
    this.lastId = lastId;
  }

  public List<ClusterManagedScriptEntryStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<ClusterManagedScriptEntryStatus> scripts) {
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
    if (!(obj instanceof ClusterManagedSqlStatus)) {
      return false;
    }
    ClusterManagedSqlStatus other = (ClusterManagedSqlStatus) obj;
    return Objects.equals(lastId, other.lastId) && Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
