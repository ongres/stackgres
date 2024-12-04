/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterPodStatus;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DistributedLogsStatus {

  private List<DistributedLogsCondition> conditions = new ArrayList<>();

  private List<ClusterPodStatus> podStatuses;

  private List<DistributedLogsStatusDatabase> databases = new ArrayList<>();

  private List<DistributedLogsStatusCluster> connectedClusters = new ArrayList<>();

  private List<String> clusters;

  private String postgresVersion;

  private String timescaledbVersion;

  private String fluentdConfigHash;

  private String arch;

  private String os;

  private String labelPrefix;

  private Boolean oldConfigMapRemoved;

  public List<DistributedLogsCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<DistributedLogsCondition> conditions) {
    this.conditions = conditions;
  }

  public List<ClusterPodStatus> getPodStatuses() {
    return podStatuses;
  }

  public void setPodStatuses(List<ClusterPodStatus> podStatuses) {
    this.podStatuses = podStatuses;
  }

  public List<DistributedLogsStatusDatabase> getDatabases() {
    return databases;
  }

  public void setDatabases(List<DistributedLogsStatusDatabase> databases) {
    this.databases = databases;
  }

  public List<DistributedLogsStatusCluster> getConnectedClusters() {
    return connectedClusters;
  }

  public void setConnectedClusters(List<DistributedLogsStatusCluster> connectedClusters) {
    this.connectedClusters = connectedClusters;
  }

  public List<String> getClusters() {
    return clusters;
  }

  public void setClusters(List<String> clusters) {
    this.clusters = clusters;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getTimescaledbVersion() {
    return timescaledbVersion;
  }

  public void setTimescaledbVersion(String timescaledbVersion) {
    this.timescaledbVersion = timescaledbVersion;
  }

  public String getFluentdConfigHash() {
    return fluentdConfigHash;
  }

  public void setFluentdConfigHash(String fluentdConfigHash) {
    this.fluentdConfigHash = fluentdConfigHash;
  }

  public String getArch() {
    return arch;
  }

  public void setArch(String arch) {
    this.arch = arch;
  }

  public String getOs() {
    return os;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getLabelPrefix() {
    return labelPrefix;
  }

  public void setLabelPrefix(String labelPrefix) {
    this.labelPrefix = labelPrefix;
  }

  public Boolean getOldConfigMapRemoved() {
    return oldConfigMapRemoved;
  }

  public void setOldConfigMapRemoved(Boolean oldConfigMapRemoved) {
    this.oldConfigMapRemoved = oldConfigMapRemoved;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
