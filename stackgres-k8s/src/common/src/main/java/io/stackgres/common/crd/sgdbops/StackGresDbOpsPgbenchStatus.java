/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.math.BigDecimal;
import java.util.List;
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
public class StackGresDbOpsPgbenchStatus {

  private BigDecimal scaleFactor;

  private Integer transactionsProcessed;

  private StackGresDbOpsPgbenchStatusLatency latency;

  private StackGresDbOpsPgbenchStatusTransactionsPerSecond transactionsPerSecond;

  private String hdrHistogram;

  private List<StackGresDbOpsPgbenchStatusStatementLatency> statements;

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

  public String getHdrHistogram() {
    return hdrHistogram;
  }

  public void setHdrHistogram(String hdrHistogram) {
    this.hdrHistogram = hdrHistogram;
  }

  public List<StackGresDbOpsPgbenchStatusStatementLatency> getStatements() {
    return statements;
  }

  public void setStatements(List<StackGresDbOpsPgbenchStatusStatementLatency> statements) {
    this.statements = statements;
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
    return Objects.equals(hdrHistogram, other.hdrHistogram)
        && Objects.equals(latency, other.latency) && Objects.equals(scaleFactor, other.scaleFactor)
        && Objects.equals(statements, other.statements)
        && Objects.equals(transactionsPerSecond, other.transactionsPerSecond)
        && Objects.equals(transactionsProcessed, other.transactionsProcessed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hdrHistogram, latency, scaleFactor, statements, transactionsPerSecond,
        transactionsProcessed);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
