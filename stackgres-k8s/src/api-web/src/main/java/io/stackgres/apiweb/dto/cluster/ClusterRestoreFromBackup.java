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
public class ClusterRestoreFromBackup {

  @JsonProperty("uid")
  private String uid;

  @JsonProperty("pointInTimeRecovery")
  private ClusterRestorePitr pointInTimeRecovery;

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public ClusterRestorePitr getPointInTimeRecovery() {
    return pointInTimeRecovery;
  }

  public void setPointInTimeRecovery(ClusterRestorePitr pointInTimeRecovery) {
    this.pointInTimeRecovery = pointInTimeRecovery;
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
    ClusterRestoreFromBackup that = (ClusterRestoreFromBackup) o;
    return Objects.equals(uid, that.uid)
        && Objects.equals(pointInTimeRecovery, that.pointInTimeRecovery);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uid, pointInTimeRecovery);
  }
}
