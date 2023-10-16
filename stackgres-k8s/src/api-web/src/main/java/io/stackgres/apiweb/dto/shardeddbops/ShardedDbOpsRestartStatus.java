/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardeddbops;

import java.util.List;

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

}
