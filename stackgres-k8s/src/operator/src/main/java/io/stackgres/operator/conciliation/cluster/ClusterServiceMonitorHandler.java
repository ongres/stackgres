/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;

@ReconciliationScope(value = StackGresCluster.class, kind = ServiceMonitor.KIND)
@ApplicationScoped
public class ClusterServiceMonitorHandler  implements ReconciliationHandler {

  private final ResourceWriter<ServiceMonitor> serviceMonitorWriter;

  @Inject
  public ClusterServiceMonitorHandler(ResourceWriter<ServiceMonitor> serviceMonitorWriter) {
    this.serviceMonitorWriter = serviceMonitorWriter;
  }

  private static ServiceMonitor safeCast(HasMetadata resource) {
    if (!(resource instanceof ServiceMonitor)) {
      throw new IllegalArgumentException("Resource must be a StatefulSet instance");
    }
    return (ServiceMonitor) resource;
  }

  @Override
  public HasMetadata create(HasMetadata resource) {
    var sm = safeCast(resource);
    return serviceMonitorWriter.create(sm);
  }

  @Override
  public HasMetadata patch(HasMetadata newResource) {
    var sm = safeCast(newResource);
    return serviceMonitorWriter.update(sm);
  }

  @Override
  public void delete(HasMetadata resource) {
    var sm = safeCast(resource);
    serviceMonitorWriter.delete(sm);
  }

}
