/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterResources {

  private Boolean enableClusterLimitsRequirements;

  private Boolean disableResourcesRequestsSplitFromTotal;

  private Boolean failWhenTotalIsHigher;

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
  public int hashCode() {
    return Objects.hash(disableResourcesRequestsSplitFromTotal, enableClusterLimitsRequirements,
        failWhenTotalIsHigher);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterResources)) {
      return false;
    }
    StackGresClusterResources other = (StackGresClusterResources) obj;
    return Objects.equals(disableResourcesRequestsSplitFromTotal,
        other.disableResourcesRequestsSplitFromTotal)
        && Objects.equals(enableClusterLimitsRequirements, other.enableClusterLimitsRequirements)
        && Objects.equals(failWhenTotalIsHigher, other.failWhenTotalIsHigher);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
