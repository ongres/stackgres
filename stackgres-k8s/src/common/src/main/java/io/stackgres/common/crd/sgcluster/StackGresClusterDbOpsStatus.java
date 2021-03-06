/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterDbOpsStatus implements KubernetesResource {

  private static final long serialVersionUID = -1;

  @JsonProperty("majorVersionUpgrade")
  @Valid
  private StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade;

  @JsonProperty("restart")
  @Valid
  private StackGresClusterDbOpsRestartStatus restart;

  @JsonProperty("minorVersionUpgrade")
  @Valid
  private StackGresClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgrade;

  @JsonProperty("securityUpgrade")
  @Valid
  private StackGresClusterDbOpsSecurityUpgradeStatus securityUpgrade;

  public StackGresClusterDbOpsMajorVersionUpgradeStatus getMajorVersionUpgrade() {
    return majorVersionUpgrade;
  }

  public void setMajorVersionUpgrade(
      StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade) {
    this.majorVersionUpgrade = majorVersionUpgrade;
  }

  public StackGresClusterDbOpsRestartStatus getRestart() {
    return restart;
  }

  public void setRestart(StackGresClusterDbOpsRestartStatus restart) {
    this.restart = restart;
  }

  public StackGresClusterDbOpsMinorVersionUpgradeStatus getMinorVersionUpgrade() {
    return minorVersionUpgrade;
  }

  public void setMinorVersionUpgrade(
      StackGresClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgrade) {
    this.minorVersionUpgrade = minorVersionUpgrade;
  }

  public StackGresClusterDbOpsSecurityUpgradeStatus getSecurityUpgrade() {
    return securityUpgrade;
  }

  public void setSecurityUpgrade(StackGresClusterDbOpsSecurityUpgradeStatus securityUpgrade) {
    this.securityUpgrade = securityUpgrade;
  }

  @Override
  public int hashCode() {
    return Objects.hash(majorVersionUpgrade, minorVersionUpgrade, restart, securityUpgrade);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterDbOpsStatus)) {
      return false;
    }
    StackGresClusterDbOpsStatus other = (StackGresClusterDbOpsStatus) obj;
    return Objects.equals(majorVersionUpgrade, other.majorVersionUpgrade)
        && Objects.equals(minorVersionUpgrade, other.minorVersionUpgrade)
        && Objects.equals(restart, other.restart)
        && Objects.equals(securityUpgrade, other.securityUpgrade);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
