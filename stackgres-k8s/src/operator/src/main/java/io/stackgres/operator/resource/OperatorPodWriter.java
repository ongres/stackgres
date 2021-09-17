/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.resource.ResourceWriter;
import org.jetbrains.annotations.NotNull;

@Priority(1)
@Alternative
@ApplicationScoped
public class OperatorPodWriter implements ResourceWriter<Pod> {

  private final KubernetesClient client;

  @Inject
  public OperatorPodWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Pod create(@NotNull Pod resource) {
    return ((StackGresKubernetesClient) client).serverSideApply(new PatchContext.Builder()
        .withFieldManager(STACKGRES_FIELD_MANAGER)
        .withForce(true)
        .build(), resource);
  }

  @Override
  public Pod update(@NotNull Pod resource) {
    return ((StackGresKubernetesClient) client).serverSideApply(new PatchContext.Builder()
        .withFieldManager(STACKGRES_FIELD_MANAGER)
        .withForce(true)
        .build(), resource);
  }

  @Override
  public void delete(@NotNull Pod resource) {
    client.pods()
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .delete();
  }
}
