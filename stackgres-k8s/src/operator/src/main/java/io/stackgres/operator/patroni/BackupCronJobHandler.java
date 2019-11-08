/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractResourceHandler;
import io.stackgres.operatorframework.resource.PairVisitor;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

@ApplicationScoped
public class BackupCronJobHandler extends AbstractResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource.getKind().equals("CronJob")
        && resource.getMetadata().getNamespace().equals(
            config.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName() + StackGresStatefulSet.BACKUP_SUFFIX);
  }

  @Override
  public boolean equals(HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new CronJobVisitor<>(), existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new CronJobVisitor<>(), existingResource, requiredResource);
  }

  private class CronJobVisitor<T> extends ResourcePairVisitor<T> {

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
