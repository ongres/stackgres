/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

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
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterSpecAnnotations)) {
      return false;
    }
    StackGresClusterSpecAnnotations other = (StackGresClusterSpecAnnotations) obj;
    return Objects.equals(allResources, other.allResources) && Objects.equals(pods, other.pods)
        && Objects.equals(services, other.services);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allResources, pods, services);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
