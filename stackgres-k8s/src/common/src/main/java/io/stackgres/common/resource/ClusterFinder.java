/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterFinder implements CustomResourceFinder<StackGresCluster> {

  private final KubernetesClientFactory kubernetesClientFactory;

  @Inject
  public ClusterFinder(KubernetesClientFactory kubernetesClientFactory) {
    this.kubernetesClientFactory = kubernetesClientFactory;
  }

  @Override
  public Optional<StackGresCluster> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubernetesClientFactory.create()) {
      return Optional.ofNullable(client.customResources(
          StackGresCluster.class,
          StackGresClusterList.class)
          .inNamespace(namespace)
          .withName(name)
          .get());
    }
  }
}
