/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class PodMonitorSpec {

  private LabelSelector selector;
  private NamespaceSelector namespaceSelector;
  private List<Endpoint> podMetricsEndpoints;

  public LabelSelector getSelector() {
    return selector;
  }

  public void setSelector(LabelSelector selector) {
    this.selector = selector;
  }

  public NamespaceSelector getNamespaceSelector() {
    return namespaceSelector;
  }

  public void setNamespaceSelector(NamespaceSelector namespaceSelector) {
    this.namespaceSelector = namespaceSelector;
  }

  public List<Endpoint> getPodMetricsEndpoints() {
    return podMetricsEndpoints;
  }

  public void setPodMetricsEndpoints(List<Endpoint> podMetricsEndpoints) {
    this.podMetricsEndpoints = podMetricsEndpoints;
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespaceSelector, podMetricsEndpoints, selector);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PodMonitorSpec)) {
      return false;
    }
    PodMonitorSpec other = (PodMonitorSpec) obj;
    return Objects.equals(namespaceSelector, other.namespaceSelector)
        && Objects.equals(podMetricsEndpoints, other.podMetricsEndpoints)
        && Objects.equals(selector, other.selector);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
