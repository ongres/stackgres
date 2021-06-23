/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.DeployedResourceDecorator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresCluster.class, kind = "StatefulSet")
@ApplicationScoped
public class ClusterStatefulSetReconciliationHandler implements ReconciliationHandler,
    DeployedResourceDecorator {

  private final ResourceWriter<StatefulSet> statefulSetWriter;

  private final ResourceScanner<Pod> podScanner;

  private final ResourceWriter<Pod> podWriter;

  private final ResourceFinder<StatefulSet> statefulSetFinder;

  @Inject
  public ClusterStatefulSetReconciliationHandler(ResourceWriter<StatefulSet> statefulSetWriter,
                                                 ResourceScanner<Pod> podScanner,
                                                 ResourceWriter<Pod> podWriter,
                                                 ResourceFinder<StatefulSet> statefulSetFinder) {
    this.statefulSetWriter = statefulSetWriter;
    this.podScanner = podScanner;
    this.podWriter = podWriter;
    this.statefulSetFinder = statefulSetFinder;
  }

  private static StatefulSet safeCast(HasMetadata resource) {
    if (!(resource instanceof StatefulSet)) {
      throw new IllegalArgumentException("Resource must be a StatefulSet instance");
    }
    return (StatefulSet) resource;
  }

  @Override
  public HasMetadata create(HasMetadata resource) {
    StatefulSet sts = safeCast(resource);
    return statefulSetWriter.create(sts);
  }

  private StatefulSet patchSts(HasMetadata resource, Function<StatefulSet, StatefulSet> updater) {
    final StatefulSet requiredSts = safeCast(resource);
    final StatefulSetSpec spec = requiredSts.getSpec();
    final Map<String, String> podSelectorLabels = spec.getSelector().getMatchLabels();
    Map<String, String> podLabels = new HashMap<>(podSelectorLabels);
    podLabels.remove(StackGresContext.DISRUPTIBLE_KEY);

    final String namespace = resource.getMetadata().getNamespace();
    var pods = podScanner.findByLabelsAndNamespace(namespace, podLabels)
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    final int desiredReplicas = spec.getReplicas();
    final int lastReplicaIndex = desiredReplicas - 1;

    preventPrimaryToBeDisrupted(requiredSts, pods, lastReplicaIndex);

    long nonDisruptablePodsRemaining = countNonDisruptablePods(requiredSts, pods, lastReplicaIndex);
    int replicas = (int) (desiredReplicas - nonDisruptablePodsRemaining);
    spec.setReplicas(replicas);

    final StatefulSet updatedSts = updater.apply(requiredSts);

    List<Pod> disruptablePodsToPatch = fixNonDisruptablePods(requiredSts, pods, lastReplicaIndex);
    List<Pod> podAnnotationsToPatch =
        fixPodAnnotations(requiredSts, pods);
    Set<Pod> podsToPatch = Stream.concat(disruptablePodsToPatch.stream(),
        podAnnotationsToPatch.stream())
        .collect(Collectors.toSet());
    updatePodChanges(podsToPatch);

    return updatedSts;

  }

  private void updatePodChanges(Collection<Pod> pods) {
    pods.forEach(podWriter::update);
  }

  private List<Pod> fixPodAnnotations(StatefulSet requiredSts, List<Pod> pods) {
    var requiredPodAnnotations = requiredSts
        .getSpec().getTemplate().getMetadata().getAnnotations();

    return pods.stream()
        .filter(pod -> !Objects.equals(requiredPodAnnotations, pod.getMetadata().getAnnotations()))
        .peek(pod -> pod.getMetadata().setAnnotations(requiredPodAnnotations))
        .collect(Collectors.toUnmodifiableList());

  }

  @Override
  public HasMetadata patch(HasMetadata newResource, HasMetadata oldResource) {
    return patchSts(newResource, sts -> updateStatefulSet(sts, safeCast(oldResource)));
  }

  @Override
  public HasMetadata replace(HasMetadata resource) {
    return patchSts(resource, this::updateStatefulSet);
  }

  private StatefulSet updateStatefulSet(StatefulSet requiredSts) {
    final ObjectMeta metadata = requiredSts.getMetadata();
    statefulSetFinder.findByNameAndNamespace(metadata.getName(), metadata.getNamespace())
        .ifPresent(deployedSts -> requiredSts.getSpec()
            .setVolumeClaimTemplates(deployedSts.getSpec().getVolumeClaimTemplates()));
    return statefulSetWriter.update(requiredSts);
  }

  private StatefulSet updateStatefulSet(StatefulSet requiredSts, StatefulSet deployedSts) {
    try {
      return statefulSetWriter.update(requiredSts);
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 422) {
        statefulSetWriter.deleteWithoutCascading(deployedSts);
        return statefulSetWriter.create(requiredSts);
      } else {
        throw ex;
      }
    }
  }

  private void preventPrimaryToBeDisrupted(StatefulSet requiredSts,
                                           List<Pod> pods,
                                           int lastReplicaIndex) {
    pods.stream()
        .filter(this::hasRoleLabel)
        .filter(this::isRolePrimary)
        .filter(pod -> getPodIndex(requiredSts, pod) > lastReplicaIndex)
        .filter(this::isDisruptable)
        .forEach(this::makePrimaryNonDisruptable);
  }

  private boolean isDisruptable(Pod pod) {
    return pod.getMetadata().getLabels().get(StackGresContext.DISRUPTIBLE_KEY)
        .equals(StackGresContext.RIGHT_VALUE);
  }

  private boolean isRolePrimary(Pod pod) {
    return pod.getMetadata().getLabels()
        .get(StackGresContext.ROLE_KEY).equals(StackGresContext.PRIMARY_ROLE);
  }

  private boolean hasRoleLabel(Pod pod) {
    return pod.getMetadata().getLabels().containsKey(StackGresContext.ROLE_KEY);
  }

  private void makePrimaryNonDisruptable(Pod primaryPod) {
    final Map<String, String> primaryPodLabels = primaryPod.getMetadata().getLabels();
    primaryPodLabels.put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.WRONG_VALUE);
    podWriter.update(primaryPod);
  }

  private long countNonDisruptablePods(StatefulSet requiredSts,
                                       List<Pod> pods,
                                       int lastReplicaIndex) {
    return pods.stream()
        .filter(this::isNonDisruptable)
        .map(pod -> getPodIndex(requiredSts, pod))
        .filter(pod -> pod >= lastReplicaIndex)
        .count();
  }

  private boolean isNonDisruptable(Pod pod) {
    return pod.getMetadata().getLabels().get(StackGresContext.DISRUPTIBLE_KEY)
        .equals(StackGresContext.WRONG_VALUE);
  }

  private List<Pod> fixNonDisruptablePods(StatefulSet requiredSts,
                                     List<Pod> pods,
                                     int lastReplicaIndex) {
    return pods.stream()
        .filter(this::isNonDisruptable)
        .filter(pod -> {
          int podIndex = getPodIndex(requiredSts, pod);
          return podIndex < lastReplicaIndex;
        })
        .peek(pod -> pod.getMetadata().getLabels()
            .put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.RIGHT_VALUE))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public void delete(HasMetadata resource) {
    final StatefulSet requiredSts = safeCast(resource);
    statefulSetWriter.delete(requiredSts);
  }

  @Override
  public void decorate(HasMetadata resource) {
    StatefulSet sts = safeCast(resource);

    int actualReplicas = sts.getSpec().getReplicas();
    Map<String, String> stsSelectorLabels = sts.getSpec().getSelector().getMatchLabels();
    Map<String, String> nonDisruptingLabels = new HashMap<>(stsSelectorLabels);
    nonDisruptingLabels.put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.WRONG_VALUE);
    var pods = podScanner
        .findByLabelsAndNamespace(sts.getMetadata().getNamespace(), nonDisruptingLabels);
    actualReplicas += pods.size();
    sts.getSpec().setReplicas(actualReplicas);
  }

  private int getPodIndex(StatefulSet sts, Pod pod) {
    var podsPreffix = sts.getMetadata().getName() + "-";
    return Integer.parseInt(pod.getMetadata().getName().substring(podsPreffix.length()));
  }
}
