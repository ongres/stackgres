/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardeddbops;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
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

}
