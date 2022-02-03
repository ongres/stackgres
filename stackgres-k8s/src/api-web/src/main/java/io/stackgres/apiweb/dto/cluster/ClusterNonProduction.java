/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

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
public class ClusterNonProduction {

  @JsonProperty("disableClusterPodAntiAffinity")
  public Boolean disableClusterPodAntiAffinity;

  @JsonProperty("enabledFeatureGates")
  public List<String> enabledFeatureGates;

  public Boolean getDisableClusterPodAntiAffinity() {
    return disableClusterPodAntiAffinity;
  }

  public void setDisableClusterPodAntiAffinity(Boolean disableClusterPodAntiAffinity) {
    this.disableClusterPodAntiAffinity = disableClusterPodAntiAffinity;
  }

  public List<String> getEnabledFeatureGates() {
    return enabledFeatureGates;
  }

  public void setEnabledFeatureGates(List<String> enabledFeatureGates) {
    this.enabledFeatureGates = enabledFeatureGates;
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
    ClusterNonProduction that = (ClusterNonProduction) o;
    return Objects.equals(disableClusterPodAntiAffinity, that.disableClusterPodAntiAffinity)
        && Objects.equals(enabledFeatureGates, that.enabledFeatureGates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(disableClusterPodAntiAffinity, enabledFeatureGates);
  }
}
