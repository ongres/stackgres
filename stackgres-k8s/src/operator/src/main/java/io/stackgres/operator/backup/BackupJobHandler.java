/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.backup;

import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.stackgres.common.crd.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operatorframework.resource.visitor.PairVisitor;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;

@ApplicationScoped
public class BackupJobHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null
        && resource instanceof Job
        && resource.getMetadata().getNamespace().equals(
            context.getCluster().getMetadata().getNamespace())
        && context.getBackups().stream().anyMatch(backup -> resource.getMetadata().getName().equals(
            BackupJob.backupJobName(backup, context)))
        && resource.getMetadata().getOwnerReferences().stream()
        .anyMatch(owner -> owner.getKind().equals(StackGresBackupDefinition.KIND));
  }

  @Override
  public boolean equals(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new JobVisitor<>(context),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new JobVisitor<>(context),
        existingResource, requiredResource);
  }

  private class JobVisitor<T>
      extends ResourcePairVisitor<T, StackGresClusterContext> {

    public JobVisitor(StackGresClusterContext context) {
      super(context);
    }

    @Override
    public PairVisitor<HasMetadata, T> visit(
        PairVisitor<HasMetadata, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(HasMetadata::getApiVersion, HasMetadata::setApiVersion)
          .visit(HasMetadata::getKind)
          .visitWith(HasMetadata::getMetadata, HasMetadata::setMetadata,
              this::visitJobMetadata)
          .lastVisit(this::visitJob);
    }

    public PairVisitor<ObjectMeta, T> visitJobMetadata(
        PairVisitor<ObjectMeta, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(ObjectMeta::getClusterName, ObjectMeta::setClusterName)
          .visit(ObjectMeta::getDeletionGracePeriodSeconds,
              ObjectMeta::setDeletionGracePeriodSeconds)
          .visit(ObjectMeta::getName, ObjectMeta::setName)
          .visit(ObjectMeta::getNamespace, ObjectMeta::setNamespace)
          .visitList(ObjectMeta::getFinalizers, ObjectMeta::setFinalizers)
          .visitMap(ObjectMeta::getAdditionalProperties,
              additionalPropertiesSetter(
                  ObjectMeta::getAdditionalProperties,
                  ObjectMeta::setAdditionalProperty))
          .visitMapTransformed(ObjectMeta::getAnnotations, ObjectMeta::setAnnotations,
              this::leftAnnotationTransformer, this::rightAnnotationTransformer,
              HashMap<String, String>::new)
          .visitMap(ObjectMeta::getLabels, ObjectMeta::setLabels);
    }

  }

}
