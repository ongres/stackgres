/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.prometheus.ServiceMonitor;
import io.stackgres.common.prometheus.ServiceMonitorList;
import io.stackgres.common.resource.ResourceWriter;

@ApplicationScoped
public class ServiceMonitorWriter implements ResourceWriter<ServiceMonitor> {

  private final KubernetesClient client;

  @Inject
  public ServiceMonitorWriter(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public ServiceMonitor create(ServiceMonitor resource) {
    return ((StackGresKubernetesClient) client).serverSideApply(new PatchContext.Builder()
        .withFieldManager(STACKGRES_FIELD_MANAGER)
        .withForce(true)
        .build(),
        resource, Optional.empty());
  }

  @Override
  public ServiceMonitor update(ServiceMonitor resource) {
    return ((StackGresKubernetesClient) client).serverSideApply(new PatchContext.Builder()
        .withFieldManager(STACKGRES_FIELD_MANAGER)
        .withForce(true)
        .build(),
        resource, Optional.ofNullable(getServiceMonitorClient()
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .get()));
  }

  @Override
  public void delete(ServiceMonitor resource) {
    getServiceMonitorClient()
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .delete();
  }

  private MixedOperation<ServiceMonitor, ServiceMonitorList,
      Resource<ServiceMonitor>> getServiceMonitorClient() {
    return client.resources(ServiceMonitor.class, ServiceMonitorList.class);
  }
}
