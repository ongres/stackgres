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
public class ServiceMonitorSpec {

  private LabelSelector selector;
  private NamespaceSelector namespaceSelector;
  private List<PodMetricsEndpoint> endpoints;

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

  public List<PodMetricsEndpoint> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(List<PodMetricsEndpoint> endpoints) {
    this.endpoints = endpoints;
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpoints, namespaceSelector, selector);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ServiceMonitorSpec)) {
      return false;
    }
    ServiceMonitorSpec other = (ServiceMonitorSpec) obj;
    return Objects.equals(endpoints, other.endpoints)
        && Objects.equals(namespaceSelector, other.namespaceSelector)
        && Objects.equals(selector, other.selector);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
