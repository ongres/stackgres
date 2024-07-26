/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

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
public class StackGresDbOpsPgbenchStatusTransactionsPerSecond {

  private StackGresDbOpsPgbenchStatusMeasure excludingConnectionsEstablishing;

  private StackGresDbOpsPgbenchStatusMeasure includingConnectionsEstablishing;

  private StackGresDbOpsPgbenchStatusTransactionsPerSecondOverTime overTime;

  public StackGresDbOpsPgbenchStatusMeasure getExcludingConnectionsEstablishing() {
    return excludingConnectionsEstablishing;
  }

  public void setExcludingConnectionsEstablishing(
      StackGresDbOpsPgbenchStatusMeasure excludingConnectionsEstablishing) {
    this.excludingConnectionsEstablishing = excludingConnectionsEstablishing;
  }

  public StackGresDbOpsPgbenchStatusMeasure getIncludingConnectionsEstablishing() {
    return includingConnectionsEstablishing;
  }

  public void setIncludingConnectionsEstablishing(
      StackGresDbOpsPgbenchStatusMeasure includingConnectionsEstablishing) {
    this.includingConnectionsEstablishing = includingConnectionsEstablishing;
  }

  public StackGresDbOpsPgbenchStatusTransactionsPerSecondOverTime getOverTime() {
    return overTime;
  }

  public void setOverTime(StackGresDbOpsPgbenchStatusTransactionsPerSecondOverTime overTime) {
    this.overTime = overTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(excludingConnectionsEstablishing, includingConnectionsEstablishing,
        overTime);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchStatusTransactionsPerSecond)) {
      return false;
    }
    StackGresDbOpsPgbenchStatusTransactionsPerSecond other = (StackGresDbOpsPgbenchStatusTransactionsPerSecond) obj;
    return Objects.equals(excludingConnectionsEstablishing, other.excludingConnectionsEstablishing)
        && Objects.equals(includingConnectionsEstablishing, other.includingConnectionsEstablishing)
        && Objects.equals(overTime, other.overTime);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
