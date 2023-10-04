/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterDbOpsStatus {

  @Valid
  private StackGresShardedClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade;

  @Valid
  private StackGresShardedClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgrade;

  public StackGresShardedClusterDbOpsMajorVersionUpgradeStatus getMajorVersionUpgrade() {
    return majorVersionUpgrade;
  }

  public void setMajorVersionUpgrade(
      StackGresShardedClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade) {
    this.majorVersionUpgrade = majorVersionUpgrade;
  }

  public StackGresShardedClusterDbOpsMinorVersionUpgradeStatus getMinorVersionUpgrade() {
    return minorVersionUpgrade;
  }

  public void setMinorVersionUpgrade(
      StackGresShardedClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgrade) {
    this.minorVersionUpgrade = minorVersionUpgrade;
  }

  @Override
  public int hashCode() {
    return Objects.hash(majorVersionUpgrade, minorVersionUpgrade);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterDbOpsStatus)) {
      return false;
    }
    StackGresShardedClusterDbOpsStatus other = (StackGresShardedClusterDbOpsStatus) obj;
    return Objects.equals(majorVersionUpgrade, other.majorVersionUpgrade)
        && Objects.equals(minorVersionUpgrade, other.minorVersionUpgrade);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
