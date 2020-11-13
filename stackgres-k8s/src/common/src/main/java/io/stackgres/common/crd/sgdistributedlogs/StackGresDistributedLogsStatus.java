/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresDistributedLogsStatus implements KubernetesResource {

  private static final long serialVersionUID = -1L;

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Valid
  private List<StackGresDistributedLogsCondition> conditions = new ArrayList<>();

  @JsonProperty("databases")
  @Valid
  private List<StackGresDistributedLogsStatusDatabase> databases = new ArrayList<>();

  @JsonProperty("connectedClusters")
  @Valid
  private List<StackGresDistributedLogsStatusCluster> connectedClusters = new ArrayList<>();

  @JsonProperty("fluentdConfigHash")
  private String fluentdConfigHash;

  public List<StackGresDistributedLogsCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<StackGresDistributedLogsCondition> conditions) {
    this.conditions = conditions;
  }

  public List<StackGresDistributedLogsStatusDatabase> getDatabases() {
    return databases;
  }

  public void setDatabases(List<StackGresDistributedLogsStatusDatabase> databases) {
    this.databases = databases;
  }

  public List<StackGresDistributedLogsStatusCluster> getConnectedClusters() {
    return connectedClusters;
  }

  public void setConnectedClusters(List<StackGresDistributedLogsStatusCluster> connectedClusters) {
    this.connectedClusters = connectedClusters;
  }

  public String getFluentdConfigHash() {
    return fluentdConfigHash;
  }

  public void setFluentdConfigHash(String fluentdConfigHash) {
    this.fluentdConfigHash = fluentdConfigHash;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions, databases, connectedClusters, fluentdConfigHash);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDistributedLogsStatus)) {
      return false;
    }
    StackGresDistributedLogsStatus other = (StackGresDistributedLogsStatus) obj;
    return Objects.equals(conditions, other.conditions)
        && Objects.equals(databases, other.databases)
        && Objects.equals(connectedClusters, other.connectedClusters)
        && Objects.equals(fluentdConfigHash, other.fluentdConfigHash);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("conditions", conditions)
        .add("databases", databases)
        .add("connectedClusters", connectedClusters)
        .add("fluentdConfigHash", fluentdConfigHash)
        .toString();
  }

}
