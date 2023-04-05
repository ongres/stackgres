/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterDbOpsStatus {

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
