/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.Map;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterDeployedResourceScanner
    extends DeployedResourcesScanner<StackGresShardedCluster>
    implements ReconciliationOperations {

  private final KubernetesClient client;
  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusterDeployedResourceScanner(
      KubernetesClient client,
      LabelFactoryForShardedCluster labelFactory) {
    this.client = client;
    this.labelFactory = labelFactory;
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresShardedCluster config) {
    return labelFactory.genericLabels(config);
  }

  @Override
  protected KubernetesClient getClient() {
    return client;
  }

  @Override
  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations() {
    return IN_NAMESPACE_RESOURCE_OPERATIONS;
  }

  static final Map<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      IN_NAMESPACE_RESOURCE_OPERATIONS =
      Map.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>ofEntries(
          Map.entry(Secret.class, KubernetesClient::secrets),
          Map.entry(Endpoints.class, KubernetesClient::endpoints),
          Map.entry(Service.class, KubernetesClient::services),
          Map.entry(StackGresCluster.class, client -> client
              .resources(StackGresCluster.class, StackGresClusterList.class))
          );

}
