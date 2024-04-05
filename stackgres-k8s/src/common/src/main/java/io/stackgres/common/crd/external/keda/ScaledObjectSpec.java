/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.keda;

import java.util.List;
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
public class ScaledObjectSpec {

  private ScaledObjectTargetRef scaleTargetRef;

  private Integer minReplicaCount;

  private Integer maxReplicaCount;

  private Integer pollingInterval;

  private Integer cooldownPeriod;

  private List<ScaledObjectTrigger> triggers;

  public ScaledObjectTargetRef getScaleTargetRef() {
    return scaleTargetRef;
  }

  public void setScaleTargetRef(ScaledObjectTargetRef scaleTargetRef) {
    this.scaleTargetRef = scaleTargetRef;
  }

  public Integer getMinReplicaCount() {
    return minReplicaCount;
  }

  public void setMinReplicaCount(Integer minReplicaCount) {
    this.minReplicaCount = minReplicaCount;
  }

  public Integer getMaxReplicaCount() {
    return maxReplicaCount;
  }

  public void setMaxReplicaCount(Integer maxReplicaCount) {
    this.maxReplicaCount = maxReplicaCount;
  }

  public Integer getPollingInterval() {
    return pollingInterval;
  }

  public void setPollingInterval(Integer pollingInterval) {
    this.pollingInterval = pollingInterval;
  }

  public Integer getCooldownPeriod() {
    return cooldownPeriod;
  }

  public void setCooldownPeriod(Integer cooldownPeriod) {
    this.cooldownPeriod = cooldownPeriod;
  }

  public List<ScaledObjectTrigger> getTriggers() {
    return triggers;
  }

  public void setTriggers(List<ScaledObjectTrigger> triggers) {
    this.triggers = triggers;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cooldownPeriod, maxReplicaCount, minReplicaCount, pollingInterval, scaleTargetRef, triggers);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ScaledObjectSpec)) {
      return false;
    }
    ScaledObjectSpec other = (ScaledObjectSpec) obj;
    return Objects.equals(cooldownPeriod, other.cooldownPeriod)
        && Objects.equals(maxReplicaCount, other.maxReplicaCount)
        && Objects.equals(minReplicaCount, other.minReplicaCount)
        && Objects.equals(pollingInterval, other.pollingInterval)
        && Objects.equals(scaleTargetRef, other.scaleTargetRef) && Objects.equals(triggers, other.triggers);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
