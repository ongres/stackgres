/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterReplication {

  @JsonProperty("mode")
  private String mode;

  @JsonProperty("role")
  private String role;

  @JsonProperty("syncInstances")
  private Integer syncInstances;

  @JsonProperty("groups")
  private List<ClusterReplicationGroup> groups;

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Integer getSyncInstances() {
    return syncInstances;
  }

  public void setSyncInstances(Integer syncInstances) {
    this.syncInstances = syncInstances;
  }

  public List<ClusterReplicationGroup> getGroups() {
    return groups;
  }

  public void setGroups(List<ClusterReplicationGroup> groups) {
    this.groups = groups;
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
