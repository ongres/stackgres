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
public class ClusterPodStatus {

  private String name;

  private String nodeName;

  private Integer replicationGroup;

  private Boolean primary;

  private Boolean pendingRestart;

  private List<ClusterInstalledExtension> installedPostgresExtensions;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public Integer getReplicationGroup() {
    return replicationGroup;
  }

  public void setReplicationGroup(Integer replicationGroup) {
    this.replicationGroup = replicationGroup;
  }

  public Boolean getPrimary() {
    return primary;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  public Boolean getPendingRestart() {
    return pendingRestart;
  }

  public void setPendingRestart(Boolean pendingRestart) {
    this.pendingRestart = pendingRestart;
  }

  public List<ClusterInstalledExtension> getInstalledPostgresExtensions() {
    return installedPostgresExtensions;
  }

  public void setInstalledPostgresExtensions(
      List<ClusterInstalledExtension> installedPostgresExtensions) {
    this.installedPostgresExtensions = installedPostgresExtensions;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
