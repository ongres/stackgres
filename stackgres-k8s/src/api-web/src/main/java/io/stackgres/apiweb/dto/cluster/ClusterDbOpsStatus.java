/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterDbOpsStatus {

  @JsonProperty("majorVersionUpgrade")
  private ClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade;

  @JsonProperty("restart")
  private ClusterDbOpsRestartStatus restart;

  @JsonProperty("minorVersionUpgrade")
  private ClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgrade;

  @JsonProperty("securityUpgrade")
  private ClusterDbOpsSecurityUpgradeStatus securityUpgrade;

  public ClusterDbOpsMajorVersionUpgradeStatus getMajorVersionUpgrade() {
    return majorVersionUpgrade;
  }

  public void setMajorVersionUpgrade(
      ClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade) {
    this.majorVersionUpgrade = majorVersionUpgrade;
  }

  public ClusterDbOpsRestartStatus getRestart() {
    return restart;
  }

  public void setRestart(ClusterDbOpsRestartStatus restart) {
    this.restart = restart;
  }

  public ClusterDbOpsMinorVersionUpgradeStatus getMinorVersionUpgrade() {
    return minorVersionUpgrade;
  }

  public void setMinorVersionUpgrade(
      ClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgrade) {
    this.minorVersionUpgrade = minorVersionUpgrade;
  }

  public ClusterDbOpsSecurityUpgradeStatus getSecurityUpgrade() {
    return securityUpgrade;
  }

  public void setSecurityUpgrade(ClusterDbOpsSecurityUpgradeStatus securityUpgrade) {
    this.securityUpgrade = securityUpgrade;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
