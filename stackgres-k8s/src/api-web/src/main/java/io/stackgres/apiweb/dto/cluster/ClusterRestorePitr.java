/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterRestorePitr {

  @JsonProperty("restoreToTimestamp")
  private String restoreToTimestamp;

  public String getRestoreToTimestamp() {
    return restoreToTimestamp;
  }

  public void setRestoreToTimestamp(String restoreToTimestamp) {
    this.restoreToTimestamp = restoreToTimestamp;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterRestorePitr that = (ClusterRestorePitr) o;
    return Objects.equals(restoreToTimestamp, that.restoreToTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(restoreToTimestamp);
  }
}
