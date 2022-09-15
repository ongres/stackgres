/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbenchStatus {

  @JsonProperty("scaleFactor")
  private BigDecimal scaleFactor;

  @JsonProperty("transactionsProcessed")
  private Integer transactionsProcessed;

  @JsonProperty("latency")
  private DbOpsPgbenchStatusLatency latency;

  @JsonProperty("transactionsPerSecond")
  private DbOpsPgbenchStatusTransactionsPerSecond transactionsPerSecond;

  public BigDecimal getScaleFactor() {
    return scaleFactor;
  }

  public void setScaleFactor(BigDecimal scaleFactor) {
    this.scaleFactor = scaleFactor;
  }

  public Integer getTransactionsProcessed() {
    return transactionsProcessed;
  }

  public void setTransactionsProcessed(Integer transactionsProcessed) {
    this.transactionsProcessed = transactionsProcessed;
  }

  public DbOpsPgbenchStatusLatency getLatency() {
    return latency;
  }

  public void setLatency(DbOpsPgbenchStatusLatency latency) {
    this.latency = latency;
  }

  public DbOpsPgbenchStatusTransactionsPerSecond getTransactionsPerSecond() {
    return transactionsPerSecond;
  }

  public void setTransactionsPerSecond(
      DbOpsPgbenchStatusTransactionsPerSecond transactionsPerSecond) {
    this.transactionsPerSecond = transactionsPerSecond;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
