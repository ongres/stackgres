/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterFinder implements CustomResourceFinder<StackGresCluster> {

  private final KubernetesClient client;

  @Inject
  public ClusterFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<StackGresCluster> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.customResources(
        StackGresCluster.class,
        StackGresClusterList.class)
        .inNamespace(namespace)
        .withName(name)
        .get());
  }
}
