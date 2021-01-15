/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresDbOpsRestartStatus implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("primaryInstance")
  private String primaryInstance;

  @JsonProperty("initialInstances")
  private List<String> initialInstances;

  @JsonProperty("pendingToRestartInstances")
  private List<String> pendingToRestartInstances;

  @JsonProperty("restartedInstances")
  private List<String> restartedInstances;

  @JsonProperty("switchoverInitiated")
  private String switchoverInitiated;

  @JsonProperty("failure")
  private String failure;

  public String getPrimaryInstance() {
    return primaryInstance;
  }

  public void setPrimaryInstance(String primaryInstance) {
    this.primaryInstance = primaryInstance;
  }

  public List<String> getInitialInstances() {
    return initialInstances;
  }

  public void setInitialInstances(List<String> initialInstances) {
    this.initialInstances = initialInstances;
  }

  public List<String> getPendingToRestartInstances() {
    return pendingToRestartInstances;
  }

  public void setPendingToRestartInstances(List<String> pendingToRestartInstances) {
    this.pendingToRestartInstances = pendingToRestartInstances;
  }

  public List<String> getRestartedInstances() {
    return restartedInstances;
  }

  public void setRestartedInstances(List<String> restartedInstances) {
    this.restartedInstances = restartedInstances;
  }

  public String getSwitchoverInitiated() {
    return switchoverInitiated;
  }

  public void setSwitchoverInitiated(String switchoverInitiated) {
    this.switchoverInitiated = switchoverInitiated;
  }

  public String getFailure() {
    return failure;
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  @Override
  public int hashCode() {
    return Objects.hash(failure, initialInstances, pendingToRestartInstances, primaryInstance,
        restartedInstances, switchoverInitiated);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsRestartStatus)) {
      return false;
    }
    StackGresDbOpsRestartStatus other = (StackGresDbOpsRestartStatus) obj;
    return Objects.equals(failure, other.failure)
        && Objects.equals(initialInstances, other.initialInstances)
        && Objects.equals(pendingToRestartInstances, other.pendingToRestartInstances)
        && Objects.equals(primaryInstance, other.primaryInstance)
        && Objects.equals(restartedInstances, other.restartedInstances)
        && Objects.equals(switchoverInitiated, other.switchoverInitiated);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
