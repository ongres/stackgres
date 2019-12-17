/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operatorframework.resource.PairVisitor;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresStatefulSetHandler extends AbstractClusterResourceHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresStatefulSetHandler.class);

  @Override
  public boolean isHandlerForResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource instanceof StatefulSet
        && resource.getMetadata().getNamespace().equals(
            config.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName());
  }

  @Override
  public boolean equals(ResourceHandlerContext<StackGresClusterConfig> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new StatefulSetVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(ResourceHandlerContext<StackGresClusterConfig> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new StatefulSetVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  private static class StatefulSetVisitor<T>
      extends ResourcePairVisitor<T, ResourceHandlerContext<StackGresClusterConfig>> {

    public StatefulSetVisitor(
        ResourceHandlerContext<StackGresClusterConfig> resourceHandlerContext) {
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
          && isNonDisruptiblePrimaryNotExisting()) {
        LOGGER.debug("Not downscaling cluster {}.{} since there is no primary Pod "
            + " or is not marked as non disruptible",
            getContext().getConfig().getCluster().getMetadata().getNamespace(),
            getContext().getConfig().getCluster().getMetadata().getName());
        return existingReplicas;
      }

      if (existsPrimaryPodWithIndexGreaterThanRequiredReplicas(requiredReplicas)) {
        if (existingReplicas > requiredReplicas) {
          LOGGER.debug("Downscaling StatefulSet for cluster {}.{} to requested instances minus 1"
              + " since the primary Pod has an index above the maximum index for the statefulset",
              getContext().getConfig().getCluster().getMetadata().getNamespace(),
              getContext().getConfig().getCluster().getMetadata().getName());
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
          .anyMatch(existingResource -> ResourceUtil.extractPodIndex(
              getContext().getConfig().getCluster(),
              existingResource.getMetadata()) >= requiredReplicas);
    }

    public boolean isPrimary(HasMetadata existingResource) {
      return existingResource instanceof Pod
          && ResourceUtil.isPrimary(
              existingResource.getMetadata().getLabels());
    }

    public boolean isNonDisruptiblePrimary(HasMetadata existingResource) {
      return existingResource instanceof Pod
          && ResourceUtil.isNonDisruptiblePrimary(
              existingResource.getMetadata().getLabels());
    }
  }
}
