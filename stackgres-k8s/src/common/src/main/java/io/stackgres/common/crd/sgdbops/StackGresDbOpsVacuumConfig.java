/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

public abstract class StackGresDbOpsVacuumConfig {

  protected Boolean full;

  protected Boolean freeze;

  protected Boolean analyze;

  protected Boolean disablePageSkipping;

  public Boolean getFull() {
    return full;
  }

  public void setFull(Boolean full) {
    this.full = full;
  }

  public Boolean getFreeze() {
    return freeze;
  }

  public void setFreeze(Boolean freeze) {
    this.freeze = freeze;
  }

  public Boolean getAnalyze() {
    return analyze;
  }

  public void setAnalyze(Boolean analyze) {
    this.analyze = analyze;
  }

  public Boolean getDisablePageSkipping() {
    return disablePageSkipping;
  }

  public void setDisablePageSkipping(Boolean disablePageSkipping) {
    this.disablePageSkipping = disablePageSkipping;
  }

  @Override
  public int hashCode() {
    return Objects.hash(analyze, disablePageSkipping, freeze, full);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsVacuumConfig)) {
      return false;
    }
    StackGresDbOpsVacuumConfig other = (StackGresDbOpsVacuumConfig) obj;
    return Objects.equals(analyze, other.analyze)
        && Objects.equals(disablePageSkipping, other.disablePageSkipping)
        && Objects.equals(freeze, other.freeze) && Objects.equals(full, other.full);
  }

}
