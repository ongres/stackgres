/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

public abstract class AbstractResourceHandler implements ResourceHandler {

  protected static final ImmutableMap<Class<? extends HasMetadata>,
      Function<KubernetesClient,
      MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>, ?,
          ? extends Resource<? extends HasMetadata, ?>>>> STACKGRES_RESOURCE_OPERATIONS =
      ImmutableMap.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>, ?,
              ? extends Resource<? extends HasMetadata, ?>>>>builder()
      .put(StatefulSet.class, client -> client.apps().statefulSets())
      .put(Service.class, client -> client.services())
      .put(ServiceAccount.class, client -> client.serviceAccounts())
      .put(Role.class, client -> client.rbac().roles())
      .put(RoleBinding.class, client -> client.rbac().roleBindings())
      .put(Secret.class, client -> client.secrets())
      .put(ConfigMap.class, client -> client.configMaps())
      .put(Endpoints.class, client -> client.endpoints())
      .build();

  @Override
  public boolean equals(HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(existingResource, requiredResource);
  }

  @Override
  public void registerKind() {
  }

  @Override
  public Stream<HasMetadata> getOrphanResources(KubernetesClient client,
      ImmutableList<StackGresClusterConfig> existingConfigs) {
    return Stream.empty();
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client, StackGresClusterConfig config) {
    return Stream.empty();
  }

  @Override
  public Optional<HasMetadata> find(KubernetesClient client, HasMetadata resource) {
    return Optional.ofNullable(getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .get());
  }

  @Override
  public HasMetadata create(KubernetesClient client, HasMetadata resource) {
    return getResourceOperation(client, resource).create(resource);
  }

  @Override
  public HasMetadata patch(KubernetesClient client, HasMetadata resource) {
    return getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .cascading(false)
        .patch(resource);
  }

  @Override
  public boolean delete(KubernetesClient client, HasMetadata resource) {
    return client.resource(resource).delete();
  }

  @SuppressWarnings("unchecked")
  private <T extends HasMetadata> MixedOperation<T, ? extends KubernetesResourceList<T>, ?,
      ? extends Resource<T, ?>> getResourceOperation(KubernetesClient client, T resource) {
    return (MixedOperation<T, ? extends KubernetesResourceList<T>, ?, ? extends Resource<T, ?>>)
        Optional.ofNullable(STACKGRES_RESOURCE_OPERATIONS.get(resource.getClass()))
        .map(function -> function.apply(client))
        .orElseThrow(() -> new RuntimeException("Resource of type " + resource.getKind()
            + " is not configured"));
  }

}
