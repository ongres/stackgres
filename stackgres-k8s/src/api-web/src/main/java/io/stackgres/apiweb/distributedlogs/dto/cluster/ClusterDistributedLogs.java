/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterDistributedLogs {

  @JsonProperty("sgDistributedLogs")
  private String distributedLogs;

  public String getDistributedLogs() {
    return distributedLogs;
  }

  public void setDistributedLogs(String distributedLogs) {
    this.distributedLogs = distributedLogs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(distributedLogs);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterDistributedLogs)) {
      return false;
    }
    ClusterDistributedLogs other = (ClusterDistributedLogs) obj;
    return Objects.equals(distributedLogs, other.distributedLogs);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sgDistributedLogs", distributedLogs)
        .toString();
  }
}
