/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresDbOpsPgbenchStatus implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("scaleFactor")
  private BigDecimal scaleFactor;

  @JsonProperty("transactionsProcessed")
  private Integer transactionsProcessed;

  @JsonProperty("latencyAverage")
  private BigDecimal latencyAverage;

  @JsonProperty("latencyStddev")
  private BigDecimal latencyStddev;

  @JsonProperty("tpsIncludingConnectionsEstablishing")
  private BigDecimal tpsIncludingConnectionsEstablishing;

  @JsonProperty("tpsExcludingConnectionsEstablishing")
  private BigDecimal tpsExcludingConnectionsEstablishing;

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

  public BigDecimal getLatencyAverage() {
    return latencyAverage;
  }

  public void setLatencyAverage(BigDecimal latencyAverage) {
    this.latencyAverage = latencyAverage;
  }

  public BigDecimal getLatencyStddev() {
    return latencyStddev;
  }

  public void setLatencyStddev(BigDecimal latencyStddev) {
    this.latencyStddev = latencyStddev;
  }

  public BigDecimal getTpsIncludingConnectionsEstablishing() {
    return tpsIncludingConnectionsEstablishing;
  }

  public void setTpsIncludingConnectionsEstablishing(
      BigDecimal tpsIncludingConnectionsEstablishing) {
    this.tpsIncludingConnectionsEstablishing = tpsIncludingConnectionsEstablishing;
  }

  public BigDecimal getTpsExcludingConnectionsEstablishing() {
    return tpsExcludingConnectionsEstablishing;
  }

  public void setTpsExcludingConnectionsEstablishing(
      BigDecimal tpsExcludingConnectionsEstablishing) {
    this.tpsExcludingConnectionsEstablishing = tpsExcludingConnectionsEstablishing;
  }

  @Override
  public int hashCode() {
    return Objects.hash(latencyAverage, latencyStddev, scaleFactor,
        tpsExcludingConnectionsEstablishing, tpsIncludingConnectionsEstablishing,
        transactionsProcessed);
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
    return Objects.equals(latencyAverage, other.latencyAverage)
        && Objects.equals(latencyStddev, other.latencyStddev)
        && Objects.equals(scaleFactor, other.scaleFactor)
        && Objects.equals(tpsExcludingConnectionsEstablishing,
            other.tpsExcludingConnectionsEstablishing)
        && Objects.equals(tpsIncludingConnectionsEstablishing,
            other.tpsIncludingConnectionsEstablishing)
        && Objects.equals(transactionsProcessed, other.transactionsProcessed);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
