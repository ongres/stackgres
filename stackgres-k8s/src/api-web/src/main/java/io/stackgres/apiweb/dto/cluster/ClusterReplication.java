/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterReplication {

  private String mode;

  private String role;

  private Integer syncInstances;

  private List<ClusterReplicationGroup> groups;

  private ClusterReplicationInitialization initialization;

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

  public ClusterReplicationInitialization getInitialization() {
    return initialization;
  }

  public void setInitialization(ClusterReplicationInitialization initialization) {
    this.initialization = initialization;
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
