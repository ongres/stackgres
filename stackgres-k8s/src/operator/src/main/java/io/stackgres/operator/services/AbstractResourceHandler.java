/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

public abstract class AbstractResourceHandler implements ResourceHandler {

  private static final ImmutableMap<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<?, ?, ?, ?>>> RESOURCE_OPERATIONS =
      ImmutableMap.<Class<? extends HasMetadata>,
          Function<KubernetesClient, MixedOperation<?, ?, ?, ?>>>builder()
      .put(StatefulSet.class, client -> client.apps().statefulSets())
      .put(Service.class, client -> client.configMaps())
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
  public HasMetadata create(KubernetesClient client, HasMetadata resource) {
    return client.resource(resource).createOrReplace();
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
    return client.resource(resource).cascading(false).delete();
  }

  @SuppressWarnings("unchecked")
  private <T extends HasMetadata, R extends Resource<T, ?>>
      MixedOperation<T, ?, ?, R> getResourceOperation(
          KubernetesClient client, T resource) {
    return (MixedOperation<T, ?, ?, R>)
        Optional.ofNullable(RESOURCE_OPERATIONS.get(resource.getClass()))
        .map(function -> function.apply(client))
        .orElseThrow(() -> new RuntimeException("Resource of type " + resource.getKind()
            + " is not configured"));
  }

}
