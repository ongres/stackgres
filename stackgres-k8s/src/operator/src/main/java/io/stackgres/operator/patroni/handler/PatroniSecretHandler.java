/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.handler;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operatorframework.resource.visitor.PairVisitor;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;

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
  public boolean equals(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new EndpointsVisitor<>(context),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new EndpointsVisitor<>(context),
        existingResource, requiredResource);
  }

  private static class EndpointsVisitor<T>
      extends ResourcePairVisitor<T, StackGresClusterContext> {

    public EndpointsVisitor(StackGresClusterContext context) {
      super(context);
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
          .visitMapTransformed(Secret::getData, Secret::setData,
              this::leftSecretDataTransformer, this::rightSecretDataTransformer,
              HashMap<String, String>::new)
          .visitMapTransformed(Secret::getStringData, Secret::setStringData,
              this::leftSecretDataTransformer, this::rightSecretDataTransformer,
              HashMap<String, String>::new);
    }

    protected Map.Entry<String, String> leftSecretDataTransformer(
        Map.Entry<String, String> leftAnnotation,
        Map.Entry<String, String> rightAnnotation) {
      return leftAnnotation;
    }

    protected Map.Entry<String, String> rightSecretDataTransformer(
        Map.Entry<String, String> leftAnnotation,
        Map.Entry<String, String> rightAnnotation) {
      if (leftAnnotation != null) {
        return leftAnnotation;
      }
      return rightAnnotation;
    }

  }
}
