/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterDistributedLogs {

  @JsonProperty("sgDistributedLogs")
  private String distributedLogs;

  @JsonProperty("retention")
  private String retention;

  public String getDistributedLogs() {
    return distributedLogs;
  }

  public void setDistributedLogs(String distributedLogs) {
    this.distributedLogs = distributedLogs;
  }

  public String getRetention() {
    return retention;
  }

  public void setRetention(String retention) {
    this.retention = retention;
  }

  @Override
  public int hashCode() {
    return Objects.hash(distributedLogs, retention);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterDistributedLogs)) {
      return false;
    }
    StackGresClusterDistributedLogs other = (StackGresClusterDistributedLogs) obj;
    return Objects.equals(distributedLogs, other.distributedLogs)
        && Objects.equals(retention, other.retention);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
