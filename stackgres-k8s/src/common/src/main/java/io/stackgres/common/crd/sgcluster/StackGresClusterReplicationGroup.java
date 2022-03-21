/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterReplicationGroup {

  @JsonProperty("name")
  @NotNull
  private String name;

  @JsonProperty("role")
  @ValidEnum(enumClass = StackGresReplicationRole.class, allowNulls = false,
      message = "role must be ha, ha-read, readonly or none")
  private String role;

  @JsonProperty("instances")
  @Positive(message = "You need at least 1 instance in the replication group")
  private int instances;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public int getInstances() {
    return instances;
  }

  public void setInstances(int instances) {
    this.instances = instances;
  }

  @Override
  public int hashCode() {
    return Objects.hash(instances, name, role);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterReplicationGroup)) {
      return false;
    }
    StackGresClusterReplicationGroup other = (StackGresClusterReplicationGroup) obj;
    return Objects.equals(instances, other.instances) && Objects.equals(name, other.name)
        && Objects.equals(role, other.role);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
