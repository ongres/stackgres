/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsPgbenchStatusTransactionsPerSecond implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("excludingConnectionsEstablishing")
  private StackGresDbOpsPgbenchStatusMeasure excludingConnectionsEstablishing;

  @JsonProperty("includingConnectionsEstablishing")
  private StackGresDbOpsPgbenchStatusMeasure includingConnectionsEstablishing;

  public StackGresDbOpsPgbenchStatusTransactionsPerSecond() { }

  public StackGresDbOpsPgbenchStatusTransactionsPerSecond(
      StackGresDbOpsPgbenchStatusMeasure excludingConnectionsEstablishing,
      StackGresDbOpsPgbenchStatusMeasure includingConnectionsEstablishing) {
    this.excludingConnectionsEstablishing = excludingConnectionsEstablishing;
    this.includingConnectionsEstablishing = includingConnectionsEstablishing;
  }

  @Override
  public int hashCode() {
    return Objects.hash(excludingConnectionsEstablishing, includingConnectionsEstablishing);
  }

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

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchStatusTransactionsPerSecond)) {
      return false;
    }
    StackGresDbOpsPgbenchStatusTransactionsPerSecond other =
        (StackGresDbOpsPgbenchStatusTransactionsPerSecond) obj;
    return Objects.equals(getExcludingConnectionsEstablishing(),
        other.getExcludingConnectionsEstablishing())
        && Objects.equals(getIncludingConnectionsEstablishing(),
        other.getIncludingConnectionsEstablishing());
  }
}
