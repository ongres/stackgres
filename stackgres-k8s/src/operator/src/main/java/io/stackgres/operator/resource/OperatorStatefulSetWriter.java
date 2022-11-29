/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
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
    return client.apps().statefulSets()
        .resource(resource)
        .patch(new PatchContext.Builder()
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .withFieldManager(STACKGRES_FIELD_MANAGER)
            .withForce(true)
            .build(),
            resource);
  }

  @Override
  public StatefulSet update(@NotNull StatefulSet resource) {
    return client.apps().statefulSets()
        .resource(resource)
        .patch(new PatchContext.Builder()
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .withFieldManager(STACKGRES_FIELD_MANAGER)
            .withForce(true)
            .build(),
            resource);
  }

  @Override
  public void delete(@NotNull StatefulSet resource) {
    client.apps().statefulSets()
        .resource(resource)
        .delete();
  }

  @Override
  public void deleteWithoutCascading(@NotNull StatefulSet resource) {
    client.apps().statefulSets()
        .resource(resource)
        .withPropagationPolicy(DeletionPropagation.ORPHAN)
        .delete();
  }

}
