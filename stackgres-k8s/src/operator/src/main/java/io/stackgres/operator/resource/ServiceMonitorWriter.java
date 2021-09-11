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
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorList;

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
        resource);
  }

  @Override
  public ServiceMonitor update(ServiceMonitor resource) {
    return ((StackGresKubernetesClient) client).serverSideApply(new PatchContext.Builder()
        .withFieldManager(STACKGRES_FIELD_MANAGER)
        .withForce(true)
        .build(),
        resource);
  }

  @Override
  public void delete(ServiceMonitor resource) {
    getServiceMonitorClient()
        .inNamespace(resource.getMetadata().getNamespace())
        .delete(resource);
  }

  private MixedOperation<ServiceMonitor, ServiceMonitorList,
      Resource<ServiceMonitor>> getServiceMonitorClient() {
    return client.resources(ServiceMonitor.class, ServiceMonitorList.class);
  }
}
