/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.ArrayList;
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
public class ClusterStatus {

  @JsonProperty("conditions")
  private List<ClusterCondition> conditions = new ArrayList<>();

  @JsonProperty("dbOps")
  private ClusterDbOpsStatus dbOps;

  @JsonProperty("managedSql")
  private ClusterManagedSqlStatus managedSql;

  public List<ClusterCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<ClusterCondition> conditions) {
    this.conditions = conditions;
  }

  public ClusterDbOpsStatus getDbOps() {
    return dbOps;
  }

  public void setDbOps(ClusterDbOpsStatus dbOps) {
    this.dbOps = dbOps;
  }

  public ClusterManagedSqlStatus getManagedSql() {
    return managedSql;
  }

  public void setManagedSql(ClusterManagedSqlStatus managedSql) {
    this.managedSql = managedSql;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions, dbOps, managedSql);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterStatus)) {
      return false;
    }
    ClusterStatus other = (ClusterStatus) obj;
    return Objects.equals(conditions, other.conditions) && Objects.equals(dbOps, other.dbOps)
        && Objects.equals(managedSql, other.managedSql);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
