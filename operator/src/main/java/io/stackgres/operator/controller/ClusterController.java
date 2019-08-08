/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.SgConfigMaps;
import io.stackgres.operator.resource.SgPatroniRole;
import io.stackgres.operator.resource.SgSecrets;
import io.stackgres.operator.resource.SgServices;
import io.stackgres.operator.resource.SgStatefulSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  @Inject
  SgStatefulSets sgStatefulSets;

  @Inject
  SgServices sgServices;

  @Inject
  SgConfigMaps sgConfigMaps;

  @Inject
  SgSecrets sgSecrets;

  @Inject
  SgPatroniRole sgRole;

  /**
   * Create all the infrastructure of StackGres.
   *
   * @param resource Custom Resource with the specification to create the cluster
   */
  public void newStackGresCluster(StackGresCluster resource) {
    sgRole.create(resource);
    sgSecrets.create(resource);
    sgServices.create(resource);
    sgConfigMaps.create(resource);
    sgStatefulSets.create(resource);
    LOGGER.info("Cluster created: '{}'", resource.getMetadata().getName());
  }

  public void updateStackGresCluster(StackGresCluster resource) {
    sgStatefulSets.update(resource);
    LOGGER.info("Cluster updated: '{}'", resource.getMetadata().getName());
  }

  /**
   * Delete full cluster.
   */
  public void deleteStackGresCluster(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      sgServices.delete(client, resource);
      sgStatefulSets.delete(client, resource);
      sgRole.delete(client, resource);
      sgSecrets.delete(client, resource);
      sgConfigMaps.delete(client, resource);
      LOGGER.info("Cluster deleted: '{}'", resource.getMetadata().getName());
    }
  }

}
