/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.resource;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.common.labels.LabelFactoryForCluster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultClusterResourceHandler
    extends AbstractClusterResourceHandler {

  private final LabelFactoryForCluster labelFactory;

  @Inject
  public DefaultClusterResourceHandler(
      LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client,
      StackGresClusterContext context) {
    return STACKGRES_CLUSTER_RESOURCE_OPERATIONS.values()
        .stream()
        .flatMap(resourceOperationGetter -> {
          return resourceOperationGetter.apply(client)
              .inNamespace(context.getCluster().getMetadata().getNamespace())
              .withLabels(labelFactory.genericLabels(context.getCluster()))
              .list()
              .getItems()
              .stream();
        });
  }

}
