/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

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
public class StackGresDistributedLogsNonProduction {

  public Boolean disableClusterPodAntiAffinity;

  public Boolean disablePatroniResourceRequirements;

  public Boolean disableClusterResourceRequirements;

  public Boolean enableSetPatroniCpuRequests;

  public Boolean enableSetClusterCpuRequests;

  public Boolean enableSetPatroniMemoryRequests;

  public Boolean enableSetClusterMemoryRequests;

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

  public Boolean getEnableSetPatroniCpuRequests() {
    return enableSetPatroniCpuRequests;
  }

  public void setEnableSetPatroniCpuRequests(Boolean enableSetPatroniCpuRequests) {
    this.enableSetPatroniCpuRequests = enableSetPatroniCpuRequests;
  }

  public Boolean getEnableSetClusterCpuRequests() {
    return enableSetClusterCpuRequests;
  }

  public void setEnableSetClusterCpuRequests(Boolean enableSetClusterCpuRequests) {
    this.enableSetClusterCpuRequests = enableSetClusterCpuRequests;
  }

  public Boolean getEnableSetPatroniMemoryRequests() {
    return enableSetPatroniMemoryRequests;
  }

  public void setEnableSetPatroniMemoryRequests(Boolean enableSetPatroniMemoryRequests) {
    this.enableSetPatroniMemoryRequests = enableSetPatroniMemoryRequests;
  }

  public Boolean getEnableSetClusterMemoryRequests() {
    return enableSetClusterMemoryRequests;
  }

  public void setEnableSetClusterMemoryRequests(Boolean enableSetClusterMemoryRequests) {
    this.enableSetClusterMemoryRequests = enableSetClusterMemoryRequests;
  }

  @Override
  public int hashCode() {
    return Objects.hash(disableClusterPodAntiAffinity, disableClusterResourceRequirements,
        disablePatroniResourceRequirements, enableSetClusterCpuRequests,
        enableSetClusterMemoryRequests, enableSetPatroniCpuRequests,
        enableSetPatroniMemoryRequests);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDistributedLogsNonProduction)) {
      return false;
    }
    StackGresDistributedLogsNonProduction other = (StackGresDistributedLogsNonProduction) obj;
    return Objects.equals(disableClusterPodAntiAffinity, other.disableClusterPodAntiAffinity)
        && Objects.equals(disableClusterResourceRequirements,
            other.disableClusterResourceRequirements)
        && Objects.equals(disablePatroniResourceRequirements,
            other.disablePatroniResourceRequirements)
        && Objects.equals(enableSetClusterCpuRequests, other.enableSetClusterCpuRequests)
        && Objects.equals(enableSetClusterMemoryRequests, other.enableSetClusterMemoryRequests)
        && Objects.equals(enableSetPatroniCpuRequests, other.enableSetPatroniCpuRequests)
        && Objects.equals(enableSetPatroniMemoryRequests, other.enableSetPatroniMemoryRequests);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
