/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import java.util.function.Function;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.stackgres.common.resource.ResourceWriter;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@Priority(1)
@Alternative
@ApplicationScoped
public class JobsStatefulSetWriter implements ResourceWriter<StatefulSet> {

  private final KubernetesClient client;

  @Inject
  public JobsStatefulSetWriter(KubernetesClient client) {
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
    withStatefulSetEndpoint(resource, endpoint -> endpoint
        .withPropagationPolicy(DeletionPropagation.ORPHAN)
        .delete());
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
