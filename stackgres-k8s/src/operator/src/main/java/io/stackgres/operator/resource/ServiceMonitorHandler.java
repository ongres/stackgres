/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.customresource.prometheus.Endpoint;
import io.stackgres.operator.customresource.prometheus.NamespaceSelector;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorDefinition;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorDoneable;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorList;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorSpec;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.operatorframework.resource.visitor.PairVisitor;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;

@ApplicationScoped
public class ServiceMonitorHandler
    extends AbstractPausableResourceHandler<StackGresClusterContext> {

  private LabelFactoryDelegator factoryDelegator;

  @Override
  public boolean isHandlerForResource(HasMetadata resource) {
    return ServiceMonitorDefinition.KIND.equals(resource.getKind());
  }

  @Override
  public boolean equals(StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new ServiceMonitorVisitor<>(),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new ServiceMonitorVisitor<>(),
        existingResource, requiredResource);
  }

  @Override
  public void registerKind() {
    KubernetesDeserializer.registerCustomKind(ServiceMonitorDefinition.APIVERSION,
        ServiceMonitorDefinition.KIND, ServiceMonitor.class);
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client,
      StackGresClusterContext context) {
    return getServiceMonitorClient(client)
        .map(crClient -> crClient
            .inAnyNamespace()
            .withLabels(factoryDelegator.pickFactory(context)
                .clusterCrossNamespaceLabels(context.getCluster()))
            .list()
            .getItems()
            .stream()
            .map(cr -> (HasMetadata) cr))
        .orElse(Stream.empty());
  }

  @Override
  public Optional<HasMetadata> find(KubernetesClient client, HasMetadata resource) {
    return getServiceMonitorClient(client)
        .flatMap(crClient -> Optional.ofNullable(crClient
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .get()))
        .map(cr -> (HasMetadata) cr);
  }

  @Override
  public HasMetadata create(KubernetesClient client, HasMetadata resource) {
    return getServiceMonitorClient(client)
        .map(crClient -> crClient
            .inNamespace(resource.getMetadata().getNamespace())
            .create((ServiceMonitor) resource))
        .orElse(null);
  }

  @Override
  public HasMetadata patch(KubernetesClient client, HasMetadata resource) {
    return getServiceMonitorClient(client)
        .map(crClient -> crClient
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .patch((ServiceMonitor) resource))
        .orElse(null);
  }

  @Override
  public boolean delete(KubernetesClient client, HasMetadata resource) {
    return getServiceMonitorClient(client)
        .map(crClient -> crClient
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .delete())
        .orElse(null);
  }

  private Optional<MixedOperation<ServiceMonitor, ServiceMonitorList, ServiceMonitorDoneable,
      Resource<ServiceMonitor, ServiceMonitorDoneable>>> getServiceMonitorClient(
          KubernetesClient client) {
    return ResourceUtil.getCustomResource(client, ServiceMonitorDefinition.NAME)
        .map(crd -> client.customResources(crd,
            ServiceMonitor.class,
            ServiceMonitorList.class,
            ServiceMonitorDoneable.class));
  }

  private class ServiceMonitorVisitor<T> extends ResourcePairVisitor<T, Void> {

    public ServiceMonitorVisitor() {
      super(null);
    }

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
              this::visitEndpoint)
          .visitWith(ServiceMonitorSpec::getNamespaceSelector,
              ServiceMonitorSpec::setNamespaceSelector,
              this::visitNamespaceSelector)
          .visitWith(ServiceMonitorSpec::getSelector,
              ServiceMonitorSpec::setSelector,
              this::visitLabelSelector);
    }

    public PairVisitor<Endpoint, T> visitEndpoint(
        PairVisitor<Endpoint, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(Endpoint::getPort, Endpoint::setPort);
    }

    public PairVisitor<NamespaceSelector, T> visitNamespaceSelector(
        PairVisitor<NamespaceSelector, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(NamespaceSelector::getAny, NamespaceSelector::setAny)
          .visitList(NamespaceSelector::getMatchNames, NamespaceSelector::setMatchNames);
    }

  }

  @Override
  protected <M extends HasMetadata> Function<KubernetesClient, MixedOperation<? extends HasMetadata,
        ? extends KubernetesResourceList<? extends HasMetadata>, ?,
        ? extends Resource<? extends HasMetadata, ?>>> getResourceOperations(M resource) {
    throw new UnsupportedOperationException();
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
