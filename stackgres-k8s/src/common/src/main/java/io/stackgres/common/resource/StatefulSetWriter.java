/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
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
    return client.apps().statefulSets().resource(resource).create();
  }

  @Override
  public StatefulSet update(@NotNull StatefulSet resource) {
    return client.apps().statefulSets().resource(resource).patch();
  }

  @Override
  public void delete(@NotNull StatefulSet resource) {
    client.apps().statefulSets().resource(resource).delete();
  }

  @Override
  public void deleteWithoutCascading(@NotNull StatefulSet resource) {
    client.apps().statefulSets()
        .resource(resource)
        .withPropagationPolicy(DeletionPropagation.ORPHAN)
        .delete();
  }

}
