/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
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

  @Inject
  public ClusterStatefulSetReconciliationHandler(ResourceWriter<StatefulSet> statefulSetWriter,
                                                 ResourceScanner<Pod> podScanner,
                                                 ResourceWriter<Pod> podWriter) {
    this.statefulSetWriter = statefulSetWriter;
    this.podScanner = podScanner;
    this.podWriter = podWriter;
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

  @Override
  public HasMetadata patch(HasMetadata newResource) {

    final StatefulSet requiredSts = safeCast(newResource);
    final StatefulSetSpec spec = requiredSts.getSpec();
    final Map<String, String> podSelectorLabels = spec.getSelector().getMatchLabels();
    Map<String, String> podLabels = new HashMap<>(podSelectorLabels);
    podLabels.remove(StackGresContext.DISRUPTIBLE_KEY);

    final String namespace = newResource.getMetadata().getNamespace();
    var pods = podScanner.findByLabelsAndNamespace(namespace, podLabels)
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    final int desiredReplicas = spec.getReplicas();
    final int lastReplicaIndex = desiredReplicas - 1;

    preventPrimaryToBeDisrupted(requiredSts, pods, lastReplicaIndex);

    long nonDisruptablePodsRemaining = countNonDisruptablePods(requiredSts, pods, lastReplicaIndex);
    int replicas = (int) (desiredReplicas - nonDisruptablePodsRemaining);
    spec.setReplicas(replicas);

    final StatefulSet updatedSts = statefulSetWriter.update(requiredSts);

    fixNonDisruptablePods(requiredSts, pods, lastReplicaIndex);

    return updatedSts;
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

  private void fixNonDisruptablePods(StatefulSet requiredSts,
                                     List<Pod> pods,
                                     int lastReplicaIndex) {
    pods.stream()
        .filter(this::isNonDisruptable)
        .forEach(pod -> {
          int podIndex = getPodIndex(requiredSts, pod);
          if (podIndex < lastReplicaIndex) {
            pod.getMetadata().getLabels()
                .put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.RIGHT_VALUE);
            podWriter.update(pod);
          }
        });
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
