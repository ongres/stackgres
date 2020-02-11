/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.CronJob;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;

public abstract class AbstractResourceHandler<T> implements ResourceHandler<T> {

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
      .put(Service.class, KubernetesClient::services)
      .put(ServiceAccount.class, KubernetesClient::serviceAccounts)
      .put(Role.class, client -> client.rbac().roles())
      .put(RoleBinding.class, client -> client.rbac().roleBindings())
      .put(Secret.class, KubernetesClient::secrets)
      .put(ConfigMap.class, KubernetesClient::configMaps)
      .put(Endpoints.class, KubernetesClient::endpoints)
      .put(CronJob.class, client -> client.batch().cronjobs())
      .put(Pod.class, client -> client.pods())
      .put(PersistentVolumeClaim.class, client -> client.persistentVolumeClaims())
      .build();

  @Override
  public boolean equals(ResourceHandlerContext<T> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(resourceHandlerContext,
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(ResourceHandlerContext<T> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(resourceHandlerContext,
        existingResource, requiredResource);
  }

  @Override
  public void registerKind() {
  }

  @Override
  public Stream<HasMetadata> getOrphanResources(KubernetesClient client,
      ImmutableList<T> existingContexts) {
    return Stream.empty();
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client, T context) {
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
    return getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .create(resource);
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
  private <M extends HasMetadata> MixedOperation<M, ? extends KubernetesResourceList<M>, ?,
      ? extends Resource<M, ?>> getResourceOperation(KubernetesClient client, M resource) {
    return (MixedOperation<M, ? extends KubernetesResourceList<M>, ?, ? extends Resource<M, ?>>)
        Optional.ofNullable(getResourceOperations(resource))
        .map(function -> function.apply(client))
        .orElseThrow(() -> new RuntimeException("Resource of type " + resource.getKind()
            + " is not configured"));
  }

  protected abstract <M extends HasMetadata> Function<KubernetesClient,
      MixedOperation<? extends HasMetadata, ? extends KubernetesResourceList<? extends HasMetadata>,
          ?, ? extends Resource<? extends HasMetadata, ?>>> getResourceOperations(M resource);

}
