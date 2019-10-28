/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter.customresources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ServiceMonitorSpec implements KubernetesResource {

  private static final long serialVersionUID = 2000013861182789247L;

  private LabelSelector namespaceSelector;

  private LabelSelector selector;

  private List<PrometheusPort> endpoints;

  public LabelSelector getNamespaceSelector() {
    return namespaceSelector;
  }

  public void setNamespaceSelector(LabelSelector namespaceSelector) {
    this.namespaceSelector = namespaceSelector;
  }

  public LabelSelector getSelector() {
    return selector;
  }

  public void setSelector(LabelSelector selector) {
    this.selector = selector;
  }

  public List<PrometheusPort> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(List<PrometheusPort> endpoints) {
    this.endpoints = endpoints;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("namespaceSelector", namespaceSelector)
        .add("selector", selector)
        .add("endpoints", endpoints)
        .toString();
  }

}
