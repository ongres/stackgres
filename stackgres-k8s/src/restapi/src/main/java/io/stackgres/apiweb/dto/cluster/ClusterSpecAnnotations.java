/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterSpecAnnotations {

  private Map<String, String> allResources;

  private Map<String, String> clusterPods;

  private Map<String, String> services;

  private Map<String, String> primaryService;

  private Map<String, String> replicasService;

  private Map<String, String> serviceAccount;

  public Map<String, String> getAllResources() {
    return allResources;
  }

  public void setAllResources(Map<String, String> allResources) {
    this.allResources = allResources;
  }

  public Map<String, String> getClusterPods() {
    return clusterPods;
  }

  public void setClusterPods(Map<String, String> clusterPods) {
    this.clusterPods = clusterPods;
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

  public void setReplicasService(Map<String, String> replicasService) {
    this.replicasService = replicasService;
  }

  public Map<String, String> getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(Map<String, String> serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
