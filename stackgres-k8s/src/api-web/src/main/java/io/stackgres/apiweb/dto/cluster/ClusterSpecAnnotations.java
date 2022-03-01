/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterSpecAnnotations {

  private Map<String, String> allResources;

  private Map<String, String> clusterPods;

  private Map<String, String> services;

  private Map<String, String> primaryService;

  private Map<String, String> replicasService;

  public Map<String, String> getAllResources() {
    return allResources;
  }

  public void setAllResources(Map<String, String> allResources) {
    this.allResources = allResources;
  }

  public Map<String, String> getClusterPods() {
    return clusterPods;
  }

  public void setClusterPods(Map<String, String> pods) {
    this.clusterPods = pods;
  }

  public Map<String, String> getServices() {
    return services;
  }

  public void setServices(Map<String, String> services) {
    this.services = services;
  }

  public Map<String, String> getPrimaryService() {
    return primaryService;
  }

  public void setPrimaryService(Map<String, String> primaryService) {
    this.primaryService = primaryService;
  }

  public Map<String, String> getReplicasService() {
    return replicasService;
  }

  public void setReplicasService(Map<String, String> resplicasService) {
    this.replicasService = resplicasService;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterSpecAnnotations that = (ClusterSpecAnnotations) o;
    return Objects.equals(allResources, that.allResources)
        && Objects.equals(clusterPods, that.clusterPods)
        && Objects.equals(services, that.services)
        && Objects.equals(primaryService, that.primaryService)
        && Objects.equals(replicasService, that.replicasService);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allResources, clusterPods, services, primaryService, replicasService);
  }
}
