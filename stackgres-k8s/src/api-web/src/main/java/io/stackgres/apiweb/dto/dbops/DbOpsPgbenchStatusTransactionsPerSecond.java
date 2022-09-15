/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbenchStatusTransactionsPerSecond {

  @JsonProperty("excludingConnectionsEstablishing")
  private DbOpsPgbenchStatusMeasure excludingConnectionsEstablishing;

  @JsonProperty("includingConnectionsEstablishing")
  private DbOpsPgbenchStatusMeasure includingConnectionsEstablishing;

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

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
