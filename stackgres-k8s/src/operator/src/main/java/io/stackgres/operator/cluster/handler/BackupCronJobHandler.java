/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.handler;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.CronJob;
import io.stackgres.operator.cluster.factory.ClusterStatefulSet;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operatorframework.resource.visitor.PairVisitor;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;

@ApplicationScoped
public class BackupCronJobHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null
        && resource instanceof CronJob
        && resource.getMetadata().getNamespace().equals(
            context.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            ClusterStatefulSet.backupName(context));
  }

  @Override
  public boolean equals(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new CronJobVisitor<>(context),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new CronJobVisitor<>(context),
        existingResource, requiredResource);
  }

  private static class CronJobVisitor<T>
      extends ResourcePairVisitor<T, StackGresClusterContext> {

    public CronJobVisitor(StackGresClusterContext context) {
      super(context);
    }

    @Override
    public PairVisitor<HasMetadata, T> visit(
        PairVisitor<HasMetadata, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(HasMetadata::getApiVersion, HasMetadata::setApiVersion)
          .visit(HasMetadata::getKind)
          .visitWith(HasMetadata::getMetadata, HasMetadata::setMetadata,
              this::visitCronJobMetadata)
          .lastVisit(this::visitCronJob);
    }

    public PairVisitor<ObjectMeta, T> visitCronJobMetadata(
        PairVisitor<ObjectMeta, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(ObjectMeta::getClusterName, ObjectMeta::setClusterName)
          .visit(ObjectMeta::getDeletionGracePeriodSeconds,
              ObjectMeta::setDeletionGracePeriodSeconds)
          .visit(ObjectMeta::getName, ObjectMeta::setName)
          .visit(ObjectMeta::getNamespace, ObjectMeta::setNamespace)
          .visitList(ObjectMeta::getFinalizers, ObjectMeta::setFinalizers)
          .visitMap(ObjectMeta::getAdditionalProperties)
          .visitMapTransformed(ObjectMeta::getAnnotations, ObjectMeta::setAnnotations,
              (left, right) -> left, (left, right) -> left)
          .visitMap(ObjectMeta::getLabels, ObjectMeta::setLabels);
    }

  }

}
