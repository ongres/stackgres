/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsMinorVersionUpgradeStatus extends DbOpsRestartStatus {

  @JsonProperty("sourcePostgresVersion")
  private String sourcePostgresVersion;

  @JsonProperty("targetPostgresVersion")
  private String targetPostgresVersion;

  public String getSourcePostgresVersion() {
    return sourcePostgresVersion;
  }

  public void setSourcePostgresVersion(String sourcePostgresVersion) {
    this.sourcePostgresVersion = sourcePostgresVersion;
  }

  public String getTargetPostgresVersion() {
    return targetPostgresVersion;
  }

  public void setTargetPostgresVersion(String targetPostgresVersion) {
    this.targetPostgresVersion = targetPostgresVersion;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DbOpsRestartStatus)) {
      return false;
    }
    DbOpsRestartStatus other = (DbOpsRestartStatus) obj;
    return Objects.equals(getFailure(), other.getFailure())
        && Objects.equals(getInitialInstances(), other.getInitialInstances())
        && Objects.equals(getPendingToRestartInstances(), other.getPendingToRestartInstances())
        && Objects.equals(getPrimaryInstance(), other.getPrimaryInstance())
        && Objects.equals(getRestartedInstances(), other.getRestartedInstances())
        && Objects.equals(getSwitchoverFinalized(), other.getSwitchoverFinalized())
        && Objects.equals(getSwitchoverInitiated(), other.getSwitchoverInitiated());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getFailure(), getInitialInstances(), getPendingToRestartInstances(),
        getPrimaryInstance(), getRestartedInstances(), getSwitchoverFinalized(),
        getSwitchoverInitiated());
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
