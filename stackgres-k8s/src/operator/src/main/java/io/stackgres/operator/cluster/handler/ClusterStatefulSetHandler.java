/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.handler;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operatorframework.resource.visitor.PairVisitor;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterStatefulSetHandler extends AbstractClusterResourceHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatefulSetHandler.class);

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null
        && resource instanceof StatefulSet
        && resource.getMetadata().getNamespace().equals(
            context.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            context.getCluster().getMetadata().getName());
  }

  @Override
  public boolean equals(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new StatefulSetVisitor<>(context),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(
      StackGresClusterContext context,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new StatefulSetVisitor<>(context),
        existingResource, requiredResource);
  }

  private static class StatefulSetVisitor<T>
      extends ResourcePairVisitor<T, StackGresClusterContext> {

    public StatefulSetVisitor(StackGresClusterContext context) {
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
          .lastVisit(this::visitStatefulSet);
    }

    @Override
    public PairVisitor<StatefulSet, T> visitStatefulSet(
        PairVisitor<StatefulSet, T> pairVisitor) {
      return pairVisitor.visit()
          .visitWith(StatefulSet::getSpec, StatefulSet::setSpec,
              this::visitStatefulSetSpec);
    }

    @Override
    public PairVisitor<StatefulSetSpec, T> visitStatefulSetSpec(
        PairVisitor<StatefulSetSpec, T> pairVisitor) {
      return pairVisitor.visit()
          .visit(StatefulSetSpec::getPodManagementPolicy,
              StatefulSetSpec::setPodManagementPolicy, "OrderedReady")
          .visitTransformed(StatefulSetSpec::getReplicas, StatefulSetSpec::setReplicas,
              (left, right) -> left, this::trasformRequiredReplicas)
          .visit(StatefulSetSpec::getRevisionHistoryLimit,
              StatefulSetSpec::setRevisionHistoryLimit, 10)
          .visit(StatefulSetSpec::getSelector, StatefulSetSpec::setSelector)
          .visit(StatefulSetSpec::getServiceName, StatefulSetSpec::setServiceName)
          .visit(StatefulSetSpec::getUpdateStrategy, StatefulSetSpec::setUpdateStrategy)
          .visitMap(StatefulSetSpec::getAdditionalProperties)
          .visitListWith(StatefulSetSpec::getVolumeClaimTemplates,
              StatefulSetSpec::setVolumeClaimTemplates,
              this::visitPersistentVolumeClaim)
          .visitWith(StatefulSetSpec::getTemplate, StatefulSetSpec::setTemplate,
              this::visitPodTemplateSpec);
    }

    private Integer trasformRequiredReplicas(Integer existingReplicas, Integer requiredReplicas) {
      if (existingReplicas > requiredReplicas
          && isNonDisruptiblePrimaryNotExisting()
          && existsPrimaryPodWithIndexGreaterThanRequiredReplicas(requiredReplicas)) {
        LOGGER.debug("Not downscaling cluster {}.{} since there is no primary Pod "
            + " or is not marked as non disruptible",
            getContext().getCluster().getMetadata().getNamespace(),
            getContext().getCluster().getMetadata().getName());
        return existingReplicas;
      }

      if (existsPrimaryPodWithIndexGreaterThanRequiredReplicas(requiredReplicas)) {
        if (existingReplicas > requiredReplicas) {
          LOGGER.debug("Downscaling StatefulSet for cluster {}.{} to requested instances minus 1"
              + " since the primary Pod has an index above the maximum index for the statefulset",
              getContext().getCluster().getMetadata().getNamespace(),
              getContext().getCluster().getMetadata().getName());
        }
        return requiredReplicas - 1;
      }

      return requiredReplicas;
    }

    private boolean isNonDisruptiblePrimaryNotExisting() {
      return getContext().getExistingResources().stream()
      .map(t -> t.v1)
      .noneMatch(this::isNonDisruptiblePrimary);
    }

    private boolean existsPrimaryPodWithIndexGreaterThanRequiredReplicas(
        Integer requiredReplicas) {
      return getContext().getExistingResources().stream()
          .map(t -> t.v1)
          .filter(this::isPrimary)
          .anyMatch(existingResource -> StackGresUtil.extractPodIndex(
              getContext().getCluster(),
              existingResource.getMetadata()) >= requiredReplicas);
    }

    public boolean isPrimary(HasMetadata existingResource) {
      return existingResource instanceof Pod
          && StackGresUtil.isPrimary(
              existingResource.getMetadata().getLabels());
    }

    public boolean isNonDisruptiblePrimary(HasMetadata existingResource) {
      return existingResource instanceof Pod
          && StackGresUtil.isNonDisruptiblePrimary(
              existingResource.getMetadata().getLabels());
    }
  }
}
