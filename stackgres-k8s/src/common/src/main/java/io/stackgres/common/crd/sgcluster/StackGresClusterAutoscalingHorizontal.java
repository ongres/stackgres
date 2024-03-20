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
public class StackGresClusterAutoscalingHorizontal {

  private String replicasConnectionsUsageTarget;

  private String replicasConnectionsUsageMetricType;

  private Integer cooldownPeriod;

  private Integer pollingInterval;

  public String getReplicasConnectionsUsageTarget() {
    return replicasConnectionsUsageTarget;
  }

  public void setReplicasConnectionsUsageTarget(String replicasConnectionsUsageTarget) {
    this.replicasConnectionsUsageTarget = replicasConnectionsUsageTarget;
  }

  public String getReplicasConnectionsUsageMetricType() {
    return replicasConnectionsUsageMetricType;
  }

  public void setReplicasConnectionsUsageMetricType(String replicasConnectionsUsageMetricType) {
    this.replicasConnectionsUsageMetricType = replicasConnectionsUsageMetricType;
  }

  public Integer getCooldownPeriod() {
    return cooldownPeriod;
  }

  public void setCooldownPeriod(Integer cooldownPeriod) {
    this.cooldownPeriod = cooldownPeriod;
  }

  public Integer getPollingInterval() {
    return pollingInterval;
  }

  public void setPollingInterval(Integer pollingInterval) {
    this.pollingInterval = pollingInterval;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cooldownPeriod, pollingInterval, replicasConnectionsUsageTarget,
        replicasConnectionsUsageMetricType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterAutoscalingHorizontal)) {
      return false;
    }
    StackGresClusterAutoscalingHorizontal other = (StackGresClusterAutoscalingHorizontal) obj;
    return Objects.equals(cooldownPeriod, other.cooldownPeriod)
        && Objects.equals(pollingInterval, other.pollingInterval)
        && Objects.equals(replicasConnectionsUsageTarget, other.replicasConnectionsUsageTarget)
        && Objects.equals(replicasConnectionsUsageMetricType, other.replicasConnectionsUsageMetricType);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
