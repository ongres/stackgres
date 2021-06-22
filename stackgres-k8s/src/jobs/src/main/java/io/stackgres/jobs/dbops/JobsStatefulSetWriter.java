/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import java.util.function.Function;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.resource.ResourceWriter;
import org.jetbrains.annotations.NotNull;

@Priority(1)
@Alternative
@ApplicationScoped
public class JobsStatefulSetWriter implements ResourceWriter<StatefulSet> {

  private final KubernetesClientFactory clientFactory;

  @Inject
  public JobsStatefulSetWriter(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
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
    return clientFactory.withNewClient(client -> {
      String namespace = statefulSet.getMetadata().getNamespace();
      String name = statefulSet.getMetadata().getName();
      var endpoint = client.apps().statefulSets()
          .inNamespace(namespace).withName(name);
      return func.apply(endpoint);
    });
  }
}
