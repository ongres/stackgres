/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterAutoscalingHorizontal {

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
