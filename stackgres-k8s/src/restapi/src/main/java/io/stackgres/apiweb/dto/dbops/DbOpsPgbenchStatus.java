/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbenchStatus {

  private BigDecimal scaleFactor;

  private Integer transactionsProcessed;

  private DbOpsPgbenchStatusLatency latency;

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
