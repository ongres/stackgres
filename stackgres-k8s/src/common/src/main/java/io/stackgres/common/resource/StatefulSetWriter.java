/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class StatefulSetWriter
    extends AbstractResourceWriter<StatefulSet> {

  private final KubernetesClient client;

  @Inject
  public StatefulSetWriter(KubernetesClient client) {
    super(client);
    this.client = client;
  }

  @Override
  public void deleteWithoutCascading(@NotNull StatefulSet resource, boolean dryRun) {
    client.apps().statefulSets()
        .resource(resource)
        .dryRun(dryRun)
        .withPropagationPolicy(DeletionPropagation.ORPHAN)
        .delete();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected MixedOperation<
          StatefulSet,
          StatefulSetList,
          RollableScalableResource<StatefulSet>> getResourceEndpoints(
      KubernetesClient client) {
    return client.apps().statefulSets();
  }

}
