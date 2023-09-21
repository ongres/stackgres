/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class DbOpsRestartStatus {

  private String primaryInstance;

  private List<String> initialInstances;

  private List<String> pendingToRestartInstances;

  private List<String> restartedInstances;

  private String switchoverInitiated;

  private String switchoverFinalized;

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

  public String getSwitchoverFinalized() {
    return switchoverFinalized;
  }

  public void setSwitchoverFinalized(String switchoverFinalized) {
    this.switchoverFinalized = switchoverFinalized;
  }

  public String getFailure() {
    return failure;
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DbOpsRestartStatus)) {
      return false;
    }
    DbOpsRestartStatus other = (DbOpsRestartStatus) obj;
    return Objects.equals(failure, other.failure)
        && Objects.equals(initialInstances, other.initialInstances)
        && Objects.equals(pendingToRestartInstances, other.pendingToRestartInstances)
        && Objects.equals(primaryInstance, other.primaryInstance)
        && Objects.equals(restartedInstances, other.restartedInstances)
        && Objects.equals(switchoverFinalized, other.switchoverFinalized)
        && Objects.equals(switchoverInitiated, other.switchoverInitiated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(failure, initialInstances, pendingToRestartInstances, primaryInstance,
        restartedInstances, switchoverFinalized, switchoverInitiated);
  }
}
