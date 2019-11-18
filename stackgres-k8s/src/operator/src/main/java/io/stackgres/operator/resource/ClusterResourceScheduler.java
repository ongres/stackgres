/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDoneable;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterResourceScheduler implements CustomResourceScheduler<StackGresCluster> {

  private KubernetesClientFactory factory;

  @Inject
  public ClusterResourceScheduler(KubernetesClientFactory factory) {
    this.factory = factory;
  }

  @Override
  public void create(StackGresCluster cluster) {
    try (KubernetesClient client = factory.create()) {

      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresClusterDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
          .inNamespace(cluster.getMetadata().getNamespace())
          .create(cluster);

    }
  }

  @Override
  public void update(StackGresCluster cluster) {
    try (KubernetesClient client = factory.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresClusterDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
          .inNamespace(cluster.getMetadata().getNamespace())
          .createOrReplace(cluster);
    }
  }

  @Override
  public void delete(StackGresCluster delete) {
    try (KubernetesClient client = factory.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresClusterDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
          .inNamespace(delete.getMetadata().getNamespace())
          .delete(delete);
    }
  }
}
