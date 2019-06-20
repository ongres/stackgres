/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.watcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.resource.SgConfigMaps;
import io.stackgres.resource.SgServices;
import io.stackgres.resource.SgStatefulSets;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresClusterWatcher implements Watcher<StackGresCluster> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresClusterWatcher.class);

  @Inject
  @NonNull
  SgStatefulSets sgReplicas;

  @Inject
  @NonNull
  SgServices sgServices;

  @Inject
  @NonNull
  SgConfigMaps sgConfigMaps;

  @Override
  public void eventReceived(@NonNull Action action, @NonNull StackGresCluster resource) {
    LOGGER.info("Received an event with action : {} and the following resource : {}",
        action, resource);
    String clusterName = resource.getMetadata().getName();
    switch (action) {
      case ADDED:
        addNewStackGresCluster(resource);
        break;
      case DELETED:
        LOGGER.info("Deleting the cluster with name '{}'", clusterName);
        break;
      case MODIFIED:
        LOGGER.info("Modify the cluster with name '{}'", clusterName);
        break;
      case ERROR:
        LOGGER.info("Error on the cluster with name '{}'", clusterName);
        break;
      default:
        throw new UnsupportedOperationException("Action not supported: " + action);
    }
  }

  private void addNewStackGresCluster(StackGresCluster resource) {
    String clusterName = resource.getMetadata().getName();
    LOGGER.info("Starting the cluster with name '{}'", clusterName);

    Service serv = sgServices.create(clusterName, 5432);
    LOGGER.info("Service created '{}'", serv.getMetadata().getName());

    ConfigMap cm = sgConfigMaps.create(clusterName, clusterName);
    LOGGER.info("ConfigMap created '{}'", cm.getMetadata().getName());

    StatefulSet rs = sgReplicas.create(resource);
    LOGGER.info("StatefulSet created '{}'", rs.getMetadata().getName());
  }

  @Override
  public void onClose(KubernetesClientException cause) {
    LOGGER.error("onClose was called, ", cause);
  }

}
