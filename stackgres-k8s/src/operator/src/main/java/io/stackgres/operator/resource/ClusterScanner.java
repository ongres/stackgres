/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.KubernetesScanner;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDoneable;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;

@ApplicationScoped
public class ClusterScanner implements KubernetesScanner<StackGresClusterList> {

  private KubernetesClient client;

  @Inject
  public ClusterScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<StackGresClusterList> findResources() {

    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME);

    return crd.map(cr -> client.customResources(cr,
        StackGresCluster.class,
        StackGresClusterList.class,
        StackGresClusterDoneable.class)
        .inAnyNamespace().list());
  }

  @Override
  public Optional<StackGresClusterList> findResources(String namespace) {

    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME);

    return crd.map(cr -> client.customResources(cr,
        StackGresCluster.class,
        StackGresClusterList.class,
        StackGresClusterDoneable.class)
        .inNamespace(namespace).list());
  }
}
