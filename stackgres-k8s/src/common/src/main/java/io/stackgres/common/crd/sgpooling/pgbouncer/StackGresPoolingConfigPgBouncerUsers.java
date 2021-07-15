/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling.pgbouncer;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPoolingConfigPgBouncerUsers {

  @JsonProperty("pool_mode")
  private String poolMode;

  @JsonProperty("max_user_connections")
  private Integer maxUserConnections;

  public String getPoolMode() {
    return poolMode;
  }

  public void setPoolMode(String poolMode) {
    this.poolMode = poolMode;
  }

  public Integer getMaxUserConnections() {
    return maxUserConnections;
  }

  public void setMaxUserConnections(Integer maxUserConnections) {
    this.maxUserConnections = maxUserConnections;
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxUserConnections, poolMode);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPoolingConfigPgBouncerUsers)) {
      return false;
    }
    StackGresPoolingConfigPgBouncerUsers other = (StackGresPoolingConfigPgBouncerUsers) obj;
    return Objects.equals(maxUserConnections, other.maxUserConnections)
        && Objects.equals(poolMode, other.poolMode);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
