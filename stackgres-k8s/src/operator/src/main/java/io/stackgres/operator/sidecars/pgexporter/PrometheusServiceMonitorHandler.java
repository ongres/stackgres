/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.operator.common.Kind;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operator.services.ResourceHandler;
import io.stackgres.operator.sidecars.pgexporter.customresources.PrometheusPort;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitor;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorDefinition;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorDoneable;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorList;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorSpec;
import io.stackgres.operatorframework.resource.PairVisitor;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Kind("ServiceMonitor")
@ApplicationScoped
public class PrometheusServiceMonitorHandler implements ResourceHandler {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PrometheusServiceMonitorHandler.class);

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Prometheus service monitor handler registered");
    KubernetesDeserializer.registerCustomKind(ServiceMonitorDefinition.APIVERSION,
        ServiceMonitorDefinition.KIND, ServiceMonitor.class);
  }

  @Override
  public boolean equals(HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new ServiceMonitorVisitor<>(),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new ServiceMonitorVisitor<>(),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata create(KubernetesClient client, HasMetadata resource) {

    ServiceMonitor serviceMonitor = (ServiceMonitor) resource;

    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, ServiceMonitorDefinition.NAME);

    return crd.map(cr -> {
      MixedOperation<ServiceMonitor,
          ServiceMonitorList,
          ServiceMonitorDoneable,
          Resource<ServiceMonitor,
              ServiceMonitorDoneable>> prometheusCli = client
          .customResources(cr,
              ServiceMonitor.class,
              ServiceMonitorList.class,
              ServiceMonitorDoneable.class);
      return prometheusCli.inNamespace(resource.getMetadata().getNamespace())
          .createOrReplace(serviceMonitor);
    }).orElse(null);

  }

  @Override
  public HasMetadata patch(KubernetesClient client, HasMetadata resource) {

    ServiceMonitor serviceMonitor = (ServiceMonitor) resource;

    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, ServiceMonitorDefinition.NAME);

    return crd.map(cr -> {
      MixedOperation<ServiceMonitor,
          ServiceMonitorList,
          ServiceMonitorDoneable,
          Resource<ServiceMonitor,
              ServiceMonitorDoneable>> prometheusCli = client
          .customResources(cr,
              ServiceMonitor.class,
              ServiceMonitorList.class,
              ServiceMonitorDoneable.class);
      return prometheusCli
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .patch(serviceMonitor);
    }).orElse(null);

  }

  @Override
  public boolean delete(KubernetesClient client, HasMetadata resource) {

    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, ServiceMonitorDefinition.NAME);

    return crd.map(cr -> {
      MixedOperation<ServiceMonitor,
          ServiceMonitorList,
          ServiceMonitorDoneable,
          Resource<ServiceMonitor,
              ServiceMonitorDoneable>> prometheusCli = client
          .customResources(cr,
              ServiceMonitor.class,
              ServiceMonitorList.class,
              ServiceMonitorDoneable.class);
      return prometheusCli
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .delete();
    }).orElse(false);

  }

  private class ServiceMonitorVisitor<T> extends ResourcePairVisitor<T> {

    @Override
    public PairVisitor<HasMetadata, T> visit(
        PairVisitor<HasMetadata, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(HasMetadata::getApiVersion, HasMetadata::setApiVersion)
          .visit(HasMetadata::getKind)
          .visitWith(HasMetadata::getMetadata, HasMetadata::setMetadata,
              this::visitMetadata)
          .lastVisit(this::visitServiceMonitor);
    }

    public PairVisitor<ServiceMonitor, T> visitServiceMonitor(
        PairVisitor<ServiceMonitor, T> pairVisitor) {
      return pairVisitor.visit()
          .visitWith(ServiceMonitor::getSpec, ServiceMonitor::setSpec,
              this::visitServiceMonitorSpec);
    }

    public PairVisitor<ServiceMonitorSpec, T> visitServiceMonitorSpec(
        PairVisitor<ServiceMonitorSpec, T> pairVisitor) {
      return pairVisitor.visit()
          .visitListWith(ServiceMonitorSpec::getEndpoints, ServiceMonitorSpec::setEndpoints,
              this::visitPrometheusPort)
          .visitWith(ServiceMonitorSpec::getNamespaceSelector,
              ServiceMonitorSpec::setNamespaceSelector,
              this::visitLabelSelector)
          .visitWith(ServiceMonitorSpec::getSelector,
              ServiceMonitorSpec::setSelector,
              this::visitLabelSelector);
    }

    public PairVisitor<PrometheusPort, T> visitPrometheusPort(
        PairVisitor<PrometheusPort, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(PrometheusPort::getPort, PrometheusPort::setPort);
    }

  }
}
