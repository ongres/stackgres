/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.HashMap;
import java.util.Map;
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
public class StackGresClusterSpecAnnotations {

  private Map<String, String> allResources;

  private Map<String, String> clusterPods;

  private Map<String, String> services;

  private Map<String, String> primaryService;

  private Map<String, String> replicasService;

  public Map<String, String> getAllResources() {
    if (allResources == null) {
      allResources = new HashMap<>();
    }

    return allResources;
  }

  public void setAllResources(Map<String, String> allResources) {
    this.allResources = allResources;
  }

  public Map<String, String> getClusterPods() {
    if (clusterPods == null) {
      clusterPods = new HashMap<>();
    }

    return clusterPods;
  }

  public void setClusterPods(Map<String, String> clusterPods) {
    this.clusterPods = clusterPods;
  }

  public Map<String, String> getServices() {
    if (services == null) {
      services = new HashMap<>();
    }

    return services;
  }

  public void setServices(Map<String, String> services) {
    this.services = services;
  }

  public Map<String, String> getPrimaryService() {
    if (primaryService == null) {
      primaryService = new HashMap<>();
    }

    return primaryService;
  }

  public void setPrimaryService(Map<String, String> primaryService) {
    this.primaryService = primaryService;
  }

  public Map<String, String> getReplicasService() {
    if (replicasService == null) {
      replicasService = new HashMap<>();
    }

    return replicasService;
  }

  public void setReplicasService(Map<String, String> replicasService) {
    this.replicasService = replicasService;
  }

  @Override
  public int hashCode() {
    return Objects.hash(allResources, clusterPods, primaryService, replicasService, services);
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
    return Objects.equals(allResources, other.allResources)
        && Objects.equals(clusterPods, other.clusterPods)
        && Objects.equals(primaryService, other.primaryService)
        && Objects.equals(replicasService, other.replicasService)
        && Objects.equals(services, other.services);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
