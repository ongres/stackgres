/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.patroni;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operatorframework.resource.PairVisitor;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

@ApplicationScoped
public class PatroniSecretHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null
        && resource instanceof Secret
        && resource.getMetadata().getNamespace().equals(
            context.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            context.getCluster().getMetadata().getName());
  }

  @Override
  public boolean equals(ResourceHandlerContext<StackGresClusterContext> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new EndpointsVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(ResourceHandlerContext<StackGresClusterContext> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new EndpointsVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  private static class EndpointsVisitor<T>
      extends ResourcePairVisitor<T, ResourceHandlerContext<StackGresClusterContext>> {

    public EndpointsVisitor(
        ResourceHandlerContext<StackGresClusterContext> resourceHandlerContext) {
      super(resourceHandlerContext);
    }

    @Override
    public PairVisitor<HasMetadata, T> visit(
        PairVisitor<HasMetadata, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(HasMetadata::getApiVersion, HasMetadata::setApiVersion)
          .visit(HasMetadata::getKind)
          .visitWith(HasMetadata::getMetadata, HasMetadata::setMetadata,
              this::visitMetadata)
          .lastVisit(this::visitPatroniSecret);
    }

    public PairVisitor<Secret, T> visitPatroniSecret(
        PairVisitor<Secret, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(Secret::getType, Secret::setType)
          .visitMapKeys(Secret::getData, Secret::setData)
          .visitMapKeys(Secret::getStringData, Secret::setStringData);
    }

  }
}
