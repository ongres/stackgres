/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.time.format.DateTimeParseException;
import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

public abstract class StackGresDbOpsRepackConfig {

  protected Boolean noOrder;

  protected String waitTimeout;

  protected Boolean noKillBackend;

  protected Boolean noAnalyze;

  protected Boolean excludeExtension;

  @ReferencedField("waitTimeout")
  interface WaitTimeout extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "waitTimeout must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = WaitTimeout.class)
  public boolean isWaitTimeoutValid() {
    try {
      if (waitTimeout != null) {
        return !java.time.Duration.parse(waitTimeout).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public Boolean getNoOrder() {
    return noOrder;
  }

  public void setNoOrder(Boolean noOrder) {
    this.noOrder = noOrder;
  }

  public String getWaitTimeout() {
    return waitTimeout;
  }

  public void setWaitTimeout(String waitTimeout) {
    this.waitTimeout = waitTimeout;
  }

  public Boolean getNoKillBackend() {
    return noKillBackend;
  }

  public void setNoKillBackend(Boolean noKillBackend) {
    this.noKillBackend = noKillBackend;
  }

  public Boolean getNoAnalyze() {
    return noAnalyze;
  }

  public void setNoAnalyze(Boolean noAnalyze) {
    this.noAnalyze = noAnalyze;
  }

  public Boolean getExcludeExtension() {
    return excludeExtension;
  }

  public void setExcludeExtension(Boolean excludeExtension) {
    this.excludeExtension = excludeExtension;
  }

  @Override
  public int hashCode() {
    return Objects.hash(excludeExtension, noAnalyze, noKillBackend, noOrder, waitTimeout);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsRepackConfig)) {
      return false;
    }
    StackGresDbOpsRepackConfig other = (StackGresDbOpsRepackConfig) obj;
    return Objects.equals(excludeExtension, other.excludeExtension)
        && Objects.equals(noAnalyze, other.noAnalyze)
        && Objects.equals(noKillBackend, other.noKillBackend)
        && Objects.equals(noOrder, other.noOrder) && Objects.equals(waitTimeout, other.waitTimeout);
  }

}
