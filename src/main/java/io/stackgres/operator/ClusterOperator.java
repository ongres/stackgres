/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.resource.SgClusterRoleBindings;
import io.stackgres.resource.SgConfigMaps;
import io.stackgres.resource.SgSecrets;
import io.stackgres.resource.SgServices;
import io.stackgres.resource.SgStatefulSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("initialization.fields.uninitialized")
@ApplicationScoped
public class ClusterOperator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterOperator.class);

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
  SgClusterRoleBindings sgClusterRoles;

  /**
   * Create all the infrastructure of StackGres.
   *
   * @param resource Custom Resource with the specification to create the cluster
   */
  public void newStackGresCluster(StackGresCluster resource) {
    sgClusterRoles.create(resource);
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
      sgClusterRoles.delete(client, resource);
      sgSecrets.delete(client, resource);
      sgConfigMaps.delete(client, resource);
      LOGGER.info("Cluster deleted: '{}'", resource.getMetadata().getName());
    }
  }

}
