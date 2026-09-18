/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterPostgresServices extends AdditionalProperties {

  @NotNull(message = "coordinator is required")
  private StackGresShardedClusterPostgresCoordinatorServices coordinator;

  @NotNull(message = "workers is required")
  private StackGresShardedClusterPostgresWorkersServices workers;

  @Null(message = "shards is deprecated use workers instead")
  private StackGresShardedClusterPostgresWorkersServices shards;

  public StackGresShardedClusterPostgresCoordinatorServices getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(StackGresShardedClusterPostgresCoordinatorServices coordinator) {
    this.coordinator = coordinator;
  }

  public StackGresShardedClusterPostgresWorkersServices getWorkers() {
    return workers;
  }

  @JsonIgnore
  public StackGresShardedClusterPostgresWorkersServices getWorkersOrShards() {
    return workers != null ? workers : shards;
  }

  public void setWorkers(StackGresShardedClusterPostgresWorkersServices workers) {
    this.workers = workers;
  }

  @Deprecated
  public StackGresShardedClusterPostgresWorkersServices getShards() {
    return shards;
  }

  @Deprecated
  public void setShards(StackGresShardedClusterPostgresWorkersServices shards) {
    this.shards = shards;
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinator, shards, workers);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterPostgresServices)) {
      return false;
    }
    StackGresShardedClusterPostgresServices other = (StackGresShardedClusterPostgresServices) obj;
    return Objects.equals(coordinator, other.coordinator) && Objects.equals(shards, other.shards)
        && Objects.equals(workers, other.workers);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
