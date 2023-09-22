/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterCondition;
import io.stackgres.apiweb.dto.cluster.ClusterInstalledExtension;
import io.stackgres.apiweb.dto.cluster.ClusterServiceBindingStatus;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterStatus {

  private List<ClusterCondition> conditions = new ArrayList<>();

  private List<ShardedClusterClusterStatus> clusterStatuses;

  private List<ClusterInstalledExtension> toInstallPostgresExtensions;

  private List<String> clusters;

  private ClusterServiceBindingStatus binding;

  private List<String> sgBackups;

  public List<ClusterCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<ClusterCondition> conditions) {
    this.conditions = conditions;
  }

  public List<ShardedClusterClusterStatus> getClusterStatuses() {
    return clusterStatuses;
  }

  public void setClusterStatuses(List<ShardedClusterClusterStatus> clusterStatuses) {
    this.clusterStatuses = clusterStatuses;
  }

  public List<ClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  public void setToInstallPostgresExtensions(
      List<ClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
  }

  public List<String> getClusters() {
    return clusters;
  }

  public void setClusters(List<String> clusters) {
    this.clusters = clusters;
  }

  public ClusterServiceBindingStatus getBinding() {
    return binding;
  }

  public void setBinding(ClusterServiceBindingStatus binding) {
    this.binding = binding;
  }

  public List<String> getSgBackups() {
    return sgBackups;
  }

  public void setSgBackups(List<String> sgBackups) {
    this.sgBackups = sgBackups;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
