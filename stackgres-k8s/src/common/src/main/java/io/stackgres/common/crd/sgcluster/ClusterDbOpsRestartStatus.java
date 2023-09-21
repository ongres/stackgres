/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

public abstract class ClusterDbOpsRestartStatus {

  @NotNull
  private List<String> initialInstances;

  @NotNull
  private String primaryInstance;

  public List<String> getInitialInstances() {
    return initialInstances;
  }

  public void setInitialInstances(List<String> initialInstances) {
    this.initialInstances = initialInstances;
  }

  public String getPrimaryInstance() {
    return primaryInstance;
  }

  public void setPrimaryInstance(String primaryInstance) {
    this.primaryInstance = primaryInstance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(initialInstances, primaryInstance);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterDbOpsRestartStatus)) {
      return false;
    }
    ClusterDbOpsRestartStatus other = (ClusterDbOpsRestartStatus) obj;
    return Objects.equals(initialInstances, other.initialInstances)
        && Objects.equals(primaryInstance, other.primaryInstance);
  }

}
