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

  private static final long serialVersionUID = 4714141925270158016L;

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Valid
  private List<StackGresDistributedLogsCondition> conditions = new ArrayList<>();

  public List<StackGresDistributedLogsCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<StackGresDistributedLogsCondition> conditions) {
    this.conditions = conditions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions);
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
    return Objects.equals(conditions, other.conditions);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("conditions", conditions)
        .toString();
  }

}
