/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operatorframework.resource.PairVisitor;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourcePairVisitor;

import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterStatefulSetPodHandler extends AbstractClusterResourceHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ClusterStatefulSetPodHandler.class);

  @Override
  public boolean isHandlerForResource(StackGresClusterContext context, HasMetadata resource) {
    return context != null
        && resource instanceof Pod
        && Objects.equals(resource.getMetadata().getLabels().get(ResourceUtil.CLUSTER_KEY),
            Boolean.TRUE.toString())
        && resource.getMetadata().getNamespace().equals(
            context.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().matches(ResourceUtil.getNameWithIndexPattern(
            context.getCluster().getMetadata().getName()));
  }

  @Override
  public boolean skipCreation() {
    return true;
  }

  @Override
  public boolean skipDeletion() {
    return true;
  }

  @Override
  public boolean equals(ResourceHandlerContext<StackGresClusterContext> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(new PodVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(ResourceHandlerContext<StackGresClusterContext> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(new PodVisitor<>(resourceHandlerContext),
        existingResource, requiredResource);
  }

  private static class PodVisitor<T>
      extends ResourcePairVisitor<T, ResourceHandlerContext<StackGresClusterContext>> {

    public PodVisitor(ResourceHandlerContext<StackGresClusterContext> resourceHandlerContext) {
      super(resourceHandlerContext);
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
          .visitTransformed(ObjectMeta::getLabels, ObjectMeta::setLabels,
              this::tranformExistingPodLabels,
              (leftLabels, rightlabels) -> tranformRequiredPodLabels(
                  leftLabels, rightlabels, podMetadata));
    }

    private Map<String, String> tranformExistingPodLabels(
        Map<String, String> leftLabels, Map<String, String> rightLabels) { // NOPMD
      return leftLabels
          .entrySet()
          .stream()
          .collect(ImmutableMap.toImmutableMap(e -> e.getKey(), e -> e.getValue()));
    }

    private Map<String, String> tranformRequiredPodLabels(
        Map<String, String> leftLabels, Map<String, String> rightMap, ObjectMeta podMetadata) {
      final String disruptibleValue;
      if (Objects.equals(
              leftLabels.get(ResourceUtil.ROLE_KEY),
              ResourceUtil.PRIMARY_ROLE)
          && (isPodIndexGreaterThanRequiredReplicas(podMetadata)
          || isPodIndexGreaterThanExistingReplicas(podMetadata))) {
        if (!Objects.equals(leftLabels.get(ResourceUtil.DISRUPTIBLE_KEY),
            Boolean.FALSE.toString())) {
          LOGGER.debug("Settind Pod {}.{} for cluster {}.{} as non disruptible since it is primary"
              + " and his index is above the maximum index for the StatefulSet",
              podMetadata.getNamespace(), podMetadata.getName(),
              getContext().getConfig().getCluster().getMetadata().getNamespace(),
              getContext().getConfig().getCluster().getMetadata().getName());
        }
        disruptibleValue = Boolean.FALSE.toString();
      } else {
        disruptibleValue = Boolean.TRUE.toString();
      }
      return Seq.concat(
          Seq.seq(rightMap.entrySet())
          .filter(e -> !e.getKey().equals(ResourceUtil.DISRUPTIBLE_KEY)),
          Seq.seq(leftLabels.entrySet())
          .filter(e -> !e.getKey().equals(ResourceUtil.DISRUPTIBLE_KEY))
          .filter(e -> !rightMap.containsKey(e.getKey())),
          Seq.seq(ImmutableMap.<String, String>of(
              ResourceUtil.DISRUPTIBLE_KEY, disruptibleValue)
              .entrySet())
          )
          .collect(ImmutableMap.toImmutableMap(e -> e.getKey(), e -> e.getValue()));
    }

    private boolean isPodIndexGreaterThanRequiredReplicas(ObjectMeta podMetadata) {
      return isPodIndexGreaterThanReplicas(
          getContext().getRequiredResources().stream().map(t -> t.v1),
          podMetadata);
    }

    private boolean isPodIndexGreaterThanExistingReplicas(ObjectMeta podMetadata) {
      return isPodIndexGreaterThanReplicas(
          getContext().getExistingResources().stream().map(t -> t.v1),
          podMetadata);
    }

    private boolean isPodIndexGreaterThanReplicas(Stream<HasMetadata> resources,
        ObjectMeta podMetadata) {
      return resources
      .filter(resource -> resource instanceof StatefulSet)
      .map(resource -> (StatefulSet) resource)
      .anyMatch(statefulSet -> ResourceUtil.extractPodIndex(
              getContext().getConfig().getCluster(),
              podMetadata) >= statefulSet.getSpec().getReplicas());
    }
  }
}
