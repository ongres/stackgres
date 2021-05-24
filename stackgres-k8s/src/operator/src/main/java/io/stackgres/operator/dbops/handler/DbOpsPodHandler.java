/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.handler;

import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operatorframework.resource.visitor.PairVisitor;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;

@ApplicationScoped
public class DbOpsPodHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null && context.isDbOpsPod(resource);
  }

  @Override
  public boolean skipCreation(StackGresClusterContext context, HasMetadata requiredResource) {
    return true;
  }

  @Override
  public boolean skipDeletion(StackGresClusterContext context, HasMetadata existingResource) {
    return true;
  }

  @Override
  public boolean equals(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new PodVisitor<>(context),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new PodVisitor<>(context),
        existingResource, requiredResource);
  }

  private static class PodVisitor<T>
      extends ResourcePairVisitor<T, StackGresClusterContext> {

    public PodVisitor(StackGresClusterContext context) {
      super(context);
    }

    @Override
    public PairVisitor<HasMetadata, T> visit(
        PairVisitor<HasMetadata, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(HasMetadata::getKind)
          .<ObjectMeta, T>visitWith(HasMetadata::getMetadata, HasMetadata::setMetadata,
              metadataPairVisitor -> visitMetadata(
                  metadataPairVisitor, metadataPairVisitor.getLeft()));
    }

    public PairVisitor<ObjectMeta, T> visitMetadata(
        PairVisitor<ObjectMeta, T> pairVisitor, ObjectMeta podMetadata) {
      return pairVisitor.visit()
          .visit(ObjectMeta::getName, ObjectMeta::setName)
          .visit(ObjectMeta::getNamespace, ObjectMeta::setNamespace)
          .visitMapTransformed(ObjectMeta::getAnnotations, ObjectMeta::setAnnotations,
              this::leftAnnotationTransformer, this::rightAnnotationTransformer,
              HashMap<String, String>::new);
    }
  }

}
