/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsPgbenchStatus {

  @JsonProperty("scaleFactor")
  private BigDecimal scaleFactor;

  @JsonProperty("transactionsProcessed")
  private Integer transactionsProcessed;

  @JsonProperty("latency")
  private StackGresDbOpsPgbenchStatusLatency latency;

  @JsonProperty("transactionsPerSecond")
  private StackGresDbOpsPgbenchStatusTransactionsPerSecond transactionsPerSecond;

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

  public StackGresDbOpsPgbenchStatusLatency getLatency() {
    return latency;
  }

  public void setLatency(StackGresDbOpsPgbenchStatusLatency latency) {
    this.latency = latency;
  }

  public StackGresDbOpsPgbenchStatusTransactionsPerSecond getTransactionsPerSecond() {
    return transactionsPerSecond;
  }

  public void setTransactionsPerSecond(
      StackGresDbOpsPgbenchStatusTransactionsPerSecond transactionsPerSecond) {
    this.transactionsPerSecond = transactionsPerSecond;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchStatus)) {
      return false;
    }
    StackGresDbOpsPgbenchStatus other = (StackGresDbOpsPgbenchStatus) obj;
    return Objects.equals(latency, other.latency)
        && Objects.equals(scaleFactor, other.scaleFactor)
        && Objects.equals(transactionsPerSecond,
            other.transactionsPerSecond)
        && Objects.equals(transactionsProcessed, other.transactionsProcessed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(latency, transactionsPerSecond);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
