/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbenchStatusTransactionsPerSecond {

  private DbOpsPgbenchStatusMeasure excludingConnectionsEstablishing;

  private DbOpsPgbenchStatusMeasure includingConnectionsEstablishing;

  private DbOpsPgbenchStatusTransactionsPerSecondOverTime overTime;

  public DbOpsPgbenchStatusMeasure getExcludingConnectionsEstablishing() {
    return excludingConnectionsEstablishing;
  }

  public DbOpsPgbenchStatusTransactionsPerSecond() { }

  public DbOpsPgbenchStatusTransactionsPerSecond(
      DbOpsPgbenchStatusMeasure excludingConnectionsEstablishing,
      DbOpsPgbenchStatusMeasure includingConnectionsEstablishing) {
    this.excludingConnectionsEstablishing = excludingConnectionsEstablishing;
    this.includingConnectionsEstablishing = includingConnectionsEstablishing;
  }

  public void setExcludingConnectionsEstablishing(DbOpsPgbenchStatusMeasure
      excludingConnectionsEstablishing) {
    this.excludingConnectionsEstablishing = excludingConnectionsEstablishing;
  }

  public DbOpsPgbenchStatusMeasure getIncludingConnectionsEstablishing() {
    return includingConnectionsEstablishing;
  }

  public void setIncludingConnectionsEstablishing(DbOpsPgbenchStatusMeasure
      includingConnectionsEstablishing) {
    this.includingConnectionsEstablishing = includingConnectionsEstablishing;
  }

  public DbOpsPgbenchStatusTransactionsPerSecondOverTime getOverTime() {
    return overTime;
  }

  public void setOverTime(DbOpsPgbenchStatusTransactionsPerSecondOverTime overTime) {
    this.overTime = overTime;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
