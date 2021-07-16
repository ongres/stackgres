/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterInstalledExtension;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class DistributedLogsStatus {

  @JsonProperty("conditions")
  private List<DistributedLogsCondition> conditions = new ArrayList<>();

  @JsonProperty("postgresExtensions")
  private List<ClusterInstalledExtension> postgresExtensions;

  @JsonProperty("clusters")
  private List<String> clusters;

  public List<DistributedLogsCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<DistributedLogsCondition> conditions) {
    this.conditions = conditions;
  }

  public List<ClusterInstalledExtension> getPostgresExtensions() {
    return postgresExtensions;
  }

  public void setPostgresExtensions(List<ClusterInstalledExtension> postgresExtensions) {
    this.postgresExtensions = postgresExtensions;
  }

  public List<String> getClusters() {
    return clusters;
  }

  public void setClusters(List<String> clusters) {
    this.clusters = clusters;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
