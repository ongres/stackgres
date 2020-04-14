/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.distributedlogs;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class DistributedLogsStatus implements KubernetesResource {

  private static final long serialVersionUID = 4714141925270158016L;

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<DistributedLogsCondition> conditions = new ArrayList<>();

  public List<DistributedLogsCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<DistributedLogsCondition> conditions) {
    this.conditions = conditions;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("conditions", conditions)
        .toString();
  }

}
