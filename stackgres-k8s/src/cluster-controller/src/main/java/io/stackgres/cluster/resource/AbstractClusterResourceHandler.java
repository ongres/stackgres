/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.resource;

import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.AbstractResourceHandler;

public abstract class AbstractClusterResourceHandler
    extends AbstractResourceHandler<StackGresClusterContext> {

  protected static final ImmutableMap<Class<? extends HasMetadata>,
      Function<KubernetesClient,
      MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
          ? extends Resource<? extends HasMetadata>>>>
      STACKGRES_CLUSTER_RESOURCE_OPERATIONS =
      ImmutableMap.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>builder()
      .put(ConfigMap.class, KubernetesClient::configMaps)
      .put(Pod.class, KubernetesClient::pods)
      .build();

  @Override
  protected <M extends HasMetadata> Function<KubernetesClient,
      MixedOperation<? extends HasMetadata, ? extends KubernetesResourceList<? extends HasMetadata>,
          ? extends Resource<? extends HasMetadata>>> getResourceOperations(M resource) {
    return STACKGRES_CLUSTER_RESOURCE_OPERATIONS.get(resource.getClass());
  }

}
