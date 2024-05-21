/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.DeploymentUtil;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FireAndForgetDeploymentReconciliationHandler<T extends CustomResource<?, ?>>
    extends FireAndForgetReconciliationHandler<T> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(FireAndForgetDeploymentReconciliationHandler.class);

  private final ReconciliationHandler<T> handler;

  private final ResourceFinder<Deployment> deploymentFinder;

  private final ResourceScanner<Pod> podScanner;

  protected FireAndForgetDeploymentReconciliationHandler(
      ReconciliationHandler<T> handler,
      ResourceFinder<Deployment> deploymentFinder,
      ResourceScanner<Pod> podScanner) {
    super(handler);
    this.handler = handler;
    this.deploymentFinder = deploymentFinder;
    this.podScanner = podScanner;
  }

  public FireAndForgetDeploymentReconciliationHandler() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.handler = null;
    this.deploymentFinder = null;
    this.podScanner = null;
  }

  @Override
  protected HasMetadata doCreate(T context, HasMetadata resource) {
    return concileDeployment(context, resource, (c, deployment) -> (Deployment) handler.create(c, deployment));
  }

  @Override
  protected HasMetadata doPatch(T context, HasMetadata newResource,
      HasMetadata oldResource) {
    return concileDeployment(context, newResource, this::updateDeployment);
  }

  @Override
  protected void doDelete(T context, HasMetadata resource) {
    handler.delete(context, safeCast(resource));
  }

  @Override
  protected void doDeleteWithOrphans(T context, HasMetadata resource) {
    handler.deleteWithOrphans(context, safeCast(resource));
  }

  private Deployment safeCast(HasMetadata resource) {
    if (!(resource instanceof Deployment)) {
      throw new IllegalArgumentException("Resource must be a Deployment instance");
    }
    return (Deployment) resource;
  }

  private Deployment updateDeployment(T context, Deployment requiredDeployment) {
    return KubernetesClientUtil.retryOnConflict(() -> {
      Deployment currentDeployment = deploymentFinder
          .findByNameAndNamespace(
              requiredDeployment.getMetadata().getName(),
              requiredDeployment.getMetadata().getNamespace())
          .orElseThrow();
      return (Deployment) handler.replace(context, fixDeploymentAnnotations(requiredDeployment, currentDeployment));
    });
  }

  private Deployment concileDeployment(T context, HasMetadata resource,
      BiFunction<T, Deployment, Deployment> writer) {
    final Deployment requiredDeployment = safeCast(resource);

    final String namespace = resource.getMetadata().getNamespace();

    Deployment updatedDeployment = writer.apply(context, requiredDeployment);

    DeploymentUtil.getDeploymentPodsMatchLabels(updatedDeployment)
        .ifPresent(labels -> fixPods(context, requiredDeployment, labels, namespace));

    return updatedDeployment;
  }

  private Deployment fixDeploymentAnnotations(Deployment requiredDeployment, Deployment currentDeployment) {
    var requiredDeploymentAnnotations =
        Optional.ofNullable(requiredDeployment.getMetadata().getAnnotations())
            .orElse(Map.of());

    return Optional.of(currentDeployment)
        .filter(deployment -> requiredDeploymentAnnotations.entrySet().stream()
            .anyMatch(requiredAnnotation -> Optional.ofNullable(deployment.getMetadata().getAnnotations())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .noneMatch(podAnnotation -> Objects.equals(requiredAnnotation, podAnnotation))))
        .map(deployment -> fixDeploymentAnnotations(requiredDeploymentAnnotations, deployment))
        .orElse(currentDeployment);
  }

  private Deployment fixDeploymentAnnotations(
      Map<String, String> requiredDeploymentAnnotations,
      Deployment deployment) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = deployment.getMetadata().getNamespace();
      final String deploymentName = deployment.getMetadata().getName();
      LOGGER.debug("Fixing annotations for Deployment {}.{} to {}",
          namespace, deploymentName, requiredDeploymentAnnotations);
    }
    deployment.getMetadata().setAnnotations(Optional.ofNullable(deployment.getMetadata().getAnnotations())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(annotation -> requiredDeploymentAnnotations.keySet()
            .stream().noneMatch(annotation.v1::equals))
        .append(Seq.seq(requiredDeploymentAnnotations))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)));
    return deployment;
  }

  private void fixPods(T context, Deployment requiredDeployment,
      final Map<String, String> labels, final String namespace) {
    var podsToFix = podScanner.getResourcesInNamespaceWithLabels(namespace, labels).stream()
        .sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .toList();
    List<Pod> podAnnotationsToPatch = fixPodsAnnotations(requiredDeployment, podsToFix);
    Seq.seq(podAnnotationsToPatch)
        .grouped(pod -> pod.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pod -> handler.patch(context, pod, null));
  }

  private List<Pod> fixPodsAnnotations(Deployment requiredDeployment, List<Pod> pods) {
    var requiredPodAnnotations =
        Optional.ofNullable(requiredDeployment.getSpec().getTemplate().getMetadata().getAnnotations())
            .orElse(Map.of());

    return pods.stream()
        .filter(pod -> requiredPodAnnotations.entrySet().stream()
            .anyMatch(requiredAnnotation -> Optional.ofNullable(pod.getMetadata().getAnnotations())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .noneMatch(podAnnotation -> Objects.equals(requiredAnnotation, podAnnotation))))
        .map(pod -> fixPodAnnotations(requiredPodAnnotations, pod))
        .toList();
  }

  private Pod fixPodAnnotations(Map<String, String> requiredPodAnnotations, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing annotations for Pod {}.{} for Deployment {}.{}"
          + " to {}", namespace, podName, namespace, name, requiredPodAnnotations);
    }
    pod.getMetadata().setAnnotations(Optional.ofNullable(pod.getMetadata().getAnnotations())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(annotation -> requiredPodAnnotations.keySet()
            .stream().noneMatch(annotation.v1::equals))
        .append(Seq.seq(requiredPodAnnotations))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)));
    return pod;
  }

}
