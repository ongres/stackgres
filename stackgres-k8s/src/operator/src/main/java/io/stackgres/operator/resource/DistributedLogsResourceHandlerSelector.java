/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.AbstractResourceHandlerSelector;
import io.stackgres.operatorframework.resource.ResourceHandler;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DistributedLogsResourceHandlerSelector
    extends AbstractResourceHandlerSelector<StackGresDistributedLogsContext> {

  private final Instance<ResourceHandler<StackGresDistributedLogsContext>> handlers;
  private final Instance<ResourceHandler<StackGresClusterContext>> clusterHandlers;

  @Inject
  public DistributedLogsResourceHandlerSelector(
      @Any Instance<ResourceHandler<StackGresDistributedLogsContext>> handlers,
      @Any Instance<ResourceHandler<StackGresClusterContext>> clusterHandlers) {
    this.handlers = handlers;
    this.clusterHandlers = clusterHandlers;
  }

  public DistributedLogsResourceHandlerSelector() {
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.handlers = null;
    this.clusterHandlers = null;
  }

  @Override
  protected Stream<ResourceHandler<StackGresDistributedLogsContext>> getResourceHandlers() {
    return Seq.seq(handlers)
        .append(Seq.seq(clusterHandlers)
            .map(StackGresClusterResourceHandlerWrapper::new));
  }

  @Override
  protected Optional<ResourceHandler<StackGresDistributedLogsContext>> getDefaultResourceHandler() {
    Instance<DefaultDistributedLogsResourceHandler> instance = handlers.select(
        DefaultDistributedLogsResourceHandler.class);
    return instance.isResolvable()
        ? Optional.of(instance.get())
        : Optional.empty();
  }

  private class StackGresClusterResourceHandlerWrapper
      implements ResourceHandler<StackGresDistributedLogsContext> {

    private final ResourceHandler<StackGresClusterContext> handler;

    private StackGresClusterResourceHandlerWrapper(
        ResourceHandler<StackGresClusterContext> handler) {
      this.handler = handler;
    }

    @Override
    public boolean equals(StackGresDistributedLogsContext context,
        HasMetadata existingResource, HasMetadata requiredResource) {
      return handler.equals(context, existingResource, requiredResource);
    }

    @Override
    public HasMetadata update(StackGresDistributedLogsContext context,
        HasMetadata existingResource, HasMetadata requiredResource) {
      return handler.update(context, existingResource, requiredResource);
    }

    @Override
    public void registerKind() {
      handler.registerKind();
    }

    @Override
    public boolean isManaged() {
      return handler.isManaged();
    }

    @Override
    public boolean skipCreation() {
      return handler.skipCreation();
    }

    @Override
    public boolean skipDeletion() {
      return handler.skipDeletion();
    }

    @Override
    public boolean isHandlerForResource(HasMetadata resource) {
      return handler.isHandlerForResource(resource);
    }

    @Override
    public boolean isHandlerForResource(StackGresDistributedLogsContext context,
        HasMetadata resource) {
      return handler.isHandlerForResource(context, resource);
    }

    @Override
    public Optional<HasMetadata> find(KubernetesClient client, HasMetadata resource) {
      return handler.find(client, resource);
    }

    @Override
    public HasMetadata create(KubernetesClient client, HasMetadata resource) {
      return handler.create(client, resource);
    }

    @Override
    public HasMetadata patch(KubernetesClient client, HasMetadata resource) {
      return handler.patch(client, resource);
    }

    @Override
    public boolean delete(KubernetesClient client, HasMetadata resource) {
      return handler.delete(client, resource);
    }

    @Override
    public String getContextNamespaceOf(HasMetadata resource) {
      return handler.getContextNamespaceOf(resource);
    }

    @Override
    public String getContextNameOf(HasMetadata resource) {
      return handler.getContextNameOf(resource);
    }

    @Override
    public Stream<HasMetadata> getResources(KubernetesClient client,
        StackGresDistributedLogsContext context) {
      return handler.getResources(client, context);
    }
  }
}
