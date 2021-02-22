/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorList;
import io.stackgres.operatorframework.resource.ResourceUtil;

@ApplicationScoped
public class ServiceMonitorWriter implements ResourceWriter<ServiceMonitor> {

  private final KubernetesClientFactory clientFactory;

  @Inject
  public ServiceMonitorWriter(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Override
  public ServiceMonitor create(ServiceMonitor resource) {

    return clientFactory.withNewClient(this::getServiceMonitorClient)
        .map(serviceMonitorClient -> serviceMonitorClient
            .inNamespace(resource.getMetadata().getNamespace())
            .create(resource))
        .orElseThrow();

  }

  @Override
  public ServiceMonitor update(ServiceMonitor resource) {
    return clientFactory.withNewClient(this::getServiceMonitorClient)
        .map(serviceMonitorClient -> serviceMonitorClient
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .patch(resource))
        .orElseThrow();
  }

  @Override
  public void delete(ServiceMonitor resource) {
    clientFactory.withNewClient(this::getServiceMonitorClient)
        .map(serviceMonitorClient -> serviceMonitorClient
            .inNamespace(resource.getMetadata().getNamespace())
            .delete(resource))
        .orElseThrow();

  }

  private Optional<MixedOperation<ServiceMonitor, ServiceMonitorList,
      Resource<ServiceMonitor>>> getServiceMonitorClient(
      KubernetesClient client) {
    return ResourceUtil.getCustomResource(client, CustomResource.getCRDName(ServiceMonitor.class))
        .map(crd -> client.customResources(ServiceMonitor.class,
            ServiceMonitorList.class));
  }
}
