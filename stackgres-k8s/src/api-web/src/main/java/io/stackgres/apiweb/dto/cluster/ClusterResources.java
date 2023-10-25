/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterResources {

  private Boolean enableClusterLimitsRequirements;

  private Boolean disableResourcesRequestsSplitFromTotal;

  public Boolean getEnableClusterLimitsRequirements() {
    return enableClusterLimitsRequirements;
  }

  public void setEnableClusterLimitsRequirements(Boolean enableClusterLimitsRequirements) {
    this.enableClusterLimitsRequirements = enableClusterLimitsRequirements;
  }

  public Boolean getDisableResourcesRequestsSplitFromTotal() {
    return disableResourcesRequestsSplitFromTotal;
  }

  public void setDisableResourcesRequestsSplitFromTotal(
      Boolean disableResourcesRequestsSplitFromTotal) {
    this.disableResourcesRequestsSplitFromTotal = disableResourcesRequestsSplitFromTotal;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
