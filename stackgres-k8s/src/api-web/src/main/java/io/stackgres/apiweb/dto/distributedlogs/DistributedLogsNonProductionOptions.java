/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DistributedLogsNonProductionOptions {

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
