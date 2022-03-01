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

  public List<ClusterCondition> getConditions() {
    return conditions;
  }

  public ClusterDbOpsStatus getDbOps() {
    return dbOps;
  }

  public void setDbOps(ClusterDbOpsStatus dbOps) {
    this.dbOps = dbOps;
  }

  public void setConditions(List<ClusterCondition> conditions) {
    this.conditions = conditions;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterStatus that = (ClusterStatus) o;
    return Objects.equals(conditions, that.conditions)
        && Objects.equals(dbOps, that.dbOps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions, dbOps);
  }
}
