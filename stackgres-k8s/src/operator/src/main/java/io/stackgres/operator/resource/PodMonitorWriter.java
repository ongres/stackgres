/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.stackgres.common.prometheus.PodMonitor;
import io.stackgres.common.prometheus.PodMonitorList;
import io.stackgres.common.resource.ResourceWriter;

@ApplicationScoped
public class PodMonitorWriter implements ResourceWriter<PodMonitor> {

  private final KubernetesClient client;

  @Inject
  public PodMonitorWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public PodMonitor create(PodMonitor resource) {
    return client.resources(PodMonitor.class)
        .resource(resource)
        .patch(new PatchContext.Builder()
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .withFieldManager(STACKGRES_FIELD_MANAGER)
            .withForce(true)
            .build(),
            resource);
  }

  @Override
  public PodMonitor update(PodMonitor resource) {
    return client.resources(PodMonitor.class)
        .resource(resource)
        .patch(new PatchContext.Builder()
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .withFieldManager(STACKGRES_FIELD_MANAGER)
            .withForce(true)
            .build(),
            resource);
  }

  @Override
  public void delete(PodMonitor resource) {
    getPodMonitorClient()
        .resource(resource)
        .delete();
  }

  private MixedOperation<PodMonitor, PodMonitorList,
      Resource<PodMonitor>> getPodMonitorClient() {
    return client.resources(PodMonitor.class, PodMonitorList.class);
  }
}
