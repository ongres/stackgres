/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterResources {

  private Map<String, ResourceRequirements> containers;

  private Map<String, ResourceRequirements> initContainers;

  private Boolean enableClusterLimitsRequirements;

  private Boolean disableResourcesRequestsSplitFromTotal;

  private Boolean failWhenTotalIsHigher;

  public Map<String, ResourceRequirements> getContainers() {
    return containers;
  }

  public void setContainers(Map<String, ResourceRequirements> containers) {
    this.containers = containers;
  }

  public Map<String, ResourceRequirements> getInitContainers() {
    return initContainers;
  }

  public void setInitContainers(Map<String, ResourceRequirements> initContainers) {
    this.initContainers = initContainers;
  }

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

  public Boolean getFailWhenTotalIsHigher() {
    return failWhenTotalIsHigher;
  }

  public void setFailWhenTotalIsHigher(Boolean failWhenTotalIsHigher) {
    this.failWhenTotalIsHigher = failWhenTotalIsHigher;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
