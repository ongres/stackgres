/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Deletable;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class StatefulSetWriter implements ResourceWriter<StatefulSet> {

  private final KubernetesClient client;

  @Inject
  public StatefulSetWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public StatefulSet create(@NotNull StatefulSet resource) {
    return withStatefulSetEndpoint(resource, endpoint -> endpoint.create(resource));
  }

  @Override
  public StatefulSet update(@NotNull StatefulSet resource) {
    return withStatefulSetEndpoint(resource, endpoint -> endpoint.patch(resource));
  }

  @Override
  public void delete(@NotNull StatefulSet resource) {
    withStatefulSetEndpoint(resource, Deletable::delete);
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

  private <T> T withStatefulSetEndpoint(
      StatefulSet statefulSet,
      Function<RollableScalableResource<StatefulSet>, T> func) {
    String namespace = statefulSet.getMetadata().getNamespace();
    String name = statefulSet.getMetadata().getName();
    var endpoint = client.apps().statefulSets()
        .inNamespace(namespace).withName(name);
    return func.apply(endpoint);
  }

}
