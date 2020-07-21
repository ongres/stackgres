/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterSpecAnnotations {

  private Map<String, String> allResources;

  private Map<String, String> pods;

  private Map<String, String> services;

  public Map<String, String> getAllResources() {
    return allResources;
  }

  public void setAllResources(Map<String, String> allResources) {
    this.allResources = allResources;
  }

  public Map<String, String> getPods() {
    return pods;
  }

  public void setPods(Map<String, String> pods) {
    this.pods = pods;
  }

  public Map<String, String> getServices() {
    return services;
  }

  public void setServices(Map<String, String> services) {
    this.services = services;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresClusterSpecAnnotations that = (StackGresClusterSpecAnnotations) o;
    return Objects.equals(allResources, that.allResources)
        && Objects.equals(pods, that.pods)
        && Objects.equals(services, that.services);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allResources, pods, services);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("allResources", allResources)
        .add("pods", pods)
        .add("services", services)
        .toString();
  }
}
