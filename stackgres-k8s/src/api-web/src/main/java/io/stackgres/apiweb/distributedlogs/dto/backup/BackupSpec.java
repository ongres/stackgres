/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs.dto.backup;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupSpec {

  @JsonProperty("sgCluster")
  @NotNull(message = "The cluster name is required")
  private String cluster;

  @JsonProperty("managedLifecycle")
  private Boolean managedLifecycle;

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public Boolean getManagedLifecycle() {
    return managedLifecycle;
  }

  public void setManagedLifecycle(Boolean managedLifecycle) {
    this.managedLifecycle = managedLifecycle;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("cluster", cluster)
        .add("managedLifecycle", managedLifecycle)
        .toString();
  }

}
