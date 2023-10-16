/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardeddbops;

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
public class ShardedDbOpsRestartStatus {

  private List<String> pendingToRestartSgClusters;

  private List<String> restartedSgClusters;

  private String failure;

  public List<String> getPendingToRestartSgClusters() {
    return pendingToRestartSgClusters;
  }

  public void setPendingToRestartSgClusters(List<String> pendingToRestartSgClusters) {
    this.pendingToRestartSgClusters = pendingToRestartSgClusters;
  }

  public List<String> getRestartedSgClusters() {
    return restartedSgClusters;
  }

  public void setRestartedSgClusters(List<String> restartedSgClusters) {
    this.restartedSgClusters = restartedSgClusters;
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
    if (!(obj instanceof ShardedDbOpsRestartStatus)) {
      return false;
    }
    ShardedDbOpsRestartStatus other = (ShardedDbOpsRestartStatus) obj;
    return Objects.equals(failure, other.failure)
        && Objects.equals(pendingToRestartSgClusters, other.pendingToRestartSgClusters)
        && Objects.equals(restartedSgClusters, other.restartedSgClusters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(failure, pendingToRestartSgClusters, restartedSgClusters);
  }
}
