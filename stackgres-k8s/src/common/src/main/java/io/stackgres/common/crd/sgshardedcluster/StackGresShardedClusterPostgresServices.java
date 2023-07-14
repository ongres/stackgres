/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterPostgresServices {

  @NotNull(message = "coordinator is required")
  private StackGresShardedClusterPostgresCoordinatorServices coordinator;

  @NotNull(message = "shards is required")
  private StackGresShardedClusterPostgresShardsServices shards;

  public StackGresShardedClusterPostgresCoordinatorServices getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(StackGresShardedClusterPostgresCoordinatorServices coordinator) {
    this.coordinator = coordinator;
  }

  public StackGresShardedClusterPostgresShardsServices getShards() {
    return shards;
  }

  public void setShards(StackGresShardedClusterPostgresShardsServices shards) {
    this.shards = shards;
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinator, shards);
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
    return Objects.equals(coordinator, other.coordinator) && Objects.equals(shards, other.shards);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
