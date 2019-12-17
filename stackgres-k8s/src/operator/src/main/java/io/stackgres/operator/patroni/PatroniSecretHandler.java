/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operatorframework.resource.PairVisitor;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

@ApplicationScoped
public class PatroniSecretHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource instanceof Secret
        && resource.getMetadata().getNamespace().equals(
            config.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName());
  }

  @Override
  public boolean equals(ResourceHandlerContext<StackGresClusterConfig> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new EndpointsVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(ResourceHandlerContext<StackGresClusterConfig> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new EndpointsVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  private static class EndpointsVisitor<T>
      extends ResourcePairVisitor<T, ResourceHandlerContext<StackGresClusterConfig>> {

    public EndpointsVisitor(ResourceHandlerContext<StackGresClusterConfig> resourceHandlerContext) {
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
