/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsMajorVersionUpgradeStatus {

  @JsonProperty("sourcePostgresVersion")
  private String sourcePostgresVersion;

  @JsonProperty("targetPostgresVersion")
  private String targetPostgresVersion;

  @JsonProperty("primaryInstance")
  private String primaryInstance;

  @JsonProperty("initialInstances")
  private List<String> initialInstances;

  @JsonProperty("pendingToRestartInstances")
  private List<String> pendingToRestartInstances;

  @JsonProperty("restartedInstances")
  private List<String> restartedInstances;

  @JsonProperty("failure")
  private String failure;

  public String getSourcePostgresVersion() {
    return sourcePostgresVersion;
  }

  public void setSourcePostgresVersion(String sourcePostgresVersion) {
    this.sourcePostgresVersion = sourcePostgresVersion;
  }

  public String getTargetPostgresVersion() {
    return targetPostgresVersion;
  }

  public void setTargetPostgresVersion(String targetPostgresVersion) {
    this.targetPostgresVersion = targetPostgresVersion;
  }

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

  public String getFailure() {
    return failure;
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  @Override
  public int hashCode() {
    return Objects.hash(failure, initialInstances, pendingToRestartInstances, primaryInstance,
        restartedInstances, sourcePostgresVersion, targetPostgresVersion);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsMajorVersionUpgradeStatus)) {
      return false;
    }
    StackGresDbOpsMajorVersionUpgradeStatus other = (StackGresDbOpsMajorVersionUpgradeStatus) obj;
    return Objects.equals(failure, other.failure)
        && Objects.equals(initialInstances, other.initialInstances)
        && Objects.equals(pendingToRestartInstances, other.pendingToRestartInstances)
        && Objects.equals(primaryInstance, other.primaryInstance)
        && Objects.equals(restartedInstances, other.restartedInstances)
        && Objects.equals(sourcePostgresVersion, other.sourcePostgresVersion)
        && Objects.equals(targetPostgresVersion, other.targetPostgresVersion);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
