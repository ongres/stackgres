/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

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
public class StackGresClusterObservability {

  private Boolean diableMetrics;

  private String receiver;

  private Boolean prometheusAutobind;

  public Boolean getDiableMetrics() {
    return diableMetrics;
  }

  public void setDiableMetrics(Boolean diableMetrics) {
    this.diableMetrics = diableMetrics;
  }

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }

  public Boolean getPrometheusAutobind() {
    return prometheusAutobind;
  }

  public void setPrometheusAutobind(Boolean prometheusAutobind) {
    this.prometheusAutobind = prometheusAutobind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(diableMetrics, prometheusAutobind, receiver);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterObservability)) {
      return false;
    }
    StackGresClusterObservability other = (StackGresClusterObservability) obj;
    return Objects.equals(diableMetrics, other.diableMetrics)
        && Objects.equals(prometheusAutobind, other.prometheusAutobind)
        && Objects.equals(receiver, other.receiver);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
