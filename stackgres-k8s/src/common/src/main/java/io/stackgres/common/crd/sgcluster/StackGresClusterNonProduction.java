/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnumList;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterNonProduction {

  @JsonProperty("disableClusterPodAntiAffinity")
  public Boolean disableClusterPodAntiAffinity;

  @JsonProperty("disablePatroniResourceRequirements")
  public Boolean disablePatroniResourceRequirements;

  @JsonProperty("disableClusterResourceRequirements")
  public Boolean disableClusterResourceRequirements;

  @JsonProperty("enabledFeatureGates")
  @ValidEnumList(enumClass = StackGresFeatureGates.class, allowNulls = true,
      message = "enabledFeatureGates must contain only babelfish-flavor")
  public List<String> enabledFeatureGates;

  public Boolean getDisableClusterPodAntiAffinity() {
    return disableClusterPodAntiAffinity;
  }

  public void setDisableClusterPodAntiAffinity(Boolean disableClusterPodAntiAffinity) {
    this.disableClusterPodAntiAffinity = disableClusterPodAntiAffinity;
  }

  public Boolean getDisablePatroniResourceRequirements() {
    return disablePatroniResourceRequirements;
  }

  public void setDisablePatroniResourceRequirements(Boolean disablePatroniResourceRequirements) {
    this.disablePatroniResourceRequirements = disablePatroniResourceRequirements;
  }

  public Boolean getDisableClusterResourceRequirements() {
    return disableClusterResourceRequirements;
  }

  public void setDisableClusterResourceRequirements(Boolean disableClusterResourceRequirements) {
    this.disableClusterResourceRequirements = disableClusterResourceRequirements;
  }

  public List<String> getEnabledFeatureGates() {
    return enabledFeatureGates;
  }

  public void setEnabledFeatureGates(List<String> enabledFeatureGates) {
    this.enabledFeatureGates = enabledFeatureGates;
  }

  @Override
  public int hashCode() {
    return Objects.hash(disableClusterPodAntiAffinity, disableClusterResourceRequirements,
        disablePatroniResourceRequirements, enabledFeatureGates);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterNonProduction)) {
      return false;
    }
    StackGresClusterNonProduction other = (StackGresClusterNonProduction) obj;
    return Objects.equals(disableClusterPodAntiAffinity, other.disableClusterPodAntiAffinity)
        && Objects.equals(disableClusterResourceRequirements,
            other.disableClusterResourceRequirements)
        && Objects.equals(disablePatroniResourceRequirements,
            other.disablePatroniResourceRequirements)
        && Objects.equals(enabledFeatureGates, other.enabledFeatureGates);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
