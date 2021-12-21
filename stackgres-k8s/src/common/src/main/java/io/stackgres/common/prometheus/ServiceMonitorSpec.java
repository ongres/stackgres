/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.prometheus;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public class ServiceMonitorSpec implements KubernetesResource {

  private static final long serialVersionUID = 2000013861182789247L;

  private LabelSelector selector;
  private NamespaceSelector namespaceSelector;
  private List<Endpoint> endpoints;

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

  public List<Endpoint> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(List<Endpoint> endpoints) {
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
