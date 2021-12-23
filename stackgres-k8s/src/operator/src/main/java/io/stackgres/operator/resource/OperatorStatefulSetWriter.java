/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.resource.ResourceWriter;
import org.jetbrains.annotations.NotNull;

@Priority(1)
@Alternative
@ApplicationScoped
public class OperatorStatefulSetWriter implements ResourceWriter<StatefulSet> {

  private final KubernetesClient client;

  @Inject
  public OperatorStatefulSetWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public StatefulSet create(@NotNull StatefulSet resource) {
    return getClient().serverSideApply(new PatchContext.Builder()
        .withFieldManager(STACKGRES_FIELD_MANAGER)
        .withForce(true)
        .build(), resource, Optional.empty());
  }

  @Override
  public StatefulSet update(@NotNull StatefulSet resource) {
    return getClient().serverSideApply(new PatchContext.Builder()
        .withFieldManager(STACKGRES_FIELD_MANAGER)
        .withForce(true)
        .build(), resource, Optional.ofNullable(client.apps().statefulSets()
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .get()));
  }

  private StackGresKubernetesClient getClient() {
    return (StackGresKubernetesClient) client;
  }

  @Override
  public void delete(@NotNull StatefulSet resource) {
    client.apps().statefulSets()
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .delete();
  }

  @Override
  public void deleteWithoutCascading(@NotNull StatefulSet resource) {
    String namespace = resource.getMetadata().getNamespace();
    String name = resource.getMetadata().getName();
    client.apps().statefulSets()
        .inNamespace(namespace).withName(name)
        .withPropagationPolicy(DeletionPropagation.ORPHAN)
        .delete();
  }

}
