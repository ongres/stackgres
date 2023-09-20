/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterClusterStatus {

  private String name;

  private Boolean pendingRestart;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getPendingRestart() {
    return pendingRestart;
  }

  public void setPendingRestart(Boolean pendingRestart) {
    this.pendingRestart = pendingRestart;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, pendingRestart);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ShardedClusterClusterStatus)) {
      return false;
    }
    ShardedClusterClusterStatus other = (ShardedClusterClusterStatus) obj;
    return Objects.equals(name, other.name) && Objects.equals(pendingRestart, other.pendingRestart);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
