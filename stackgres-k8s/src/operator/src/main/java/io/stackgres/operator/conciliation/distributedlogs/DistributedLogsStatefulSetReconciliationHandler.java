/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static io.stackgres.common.StackGresContext.ANNOTATIONS_TO_COMPONENT;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresDistributedLogs.class, kind = "StatefulSet")
public class DistributedLogsStatefulSetReconciliationHandler
    implements ReconciliationHandler<StackGresDistributedLogs> {

  private final ResourceWriter<StatefulSet> statefulSetWriter;

  private final ResourceScanner<Pod> podScanner;

  private final ResourceWriter<Pod> podWriter;

  private final ResourceFinder<StatefulSet> statefulSetFinder;

  @Inject
  public DistributedLogsStatefulSetReconciliationHandler(
      ResourceWriter<StatefulSet> statefulSetWriter,
      ResourceScanner<Pod> podScanner,
      ResourceWriter<Pod> podWriter, ResourceFinder<StatefulSet> statefulSetFinder) {
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
  public HasMetadata create(StackGresDistributedLogs context, HasMetadata resource) {
    StatefulSet sts = safeCast(resource);
    return statefulSetWriter.create(sts);
  }

  @Override
  public HasMetadata patch(StackGresDistributedLogs context, HasMetadata newResource,
      HasMetadata oldResource) {
    var newSts = safeCast(newResource);
    var oldSts = safeCast(oldResource);
    var updatedSts = updateStatefulSet(newSts, oldSts);
    var pods = getDeployedPods(newSts);
    var podsToUpdate = fixPodAnnotations(newSts, pods);
    podsToUpdate.forEach(podWriter::update);
    return updatedSts;
  }

  @Override
  public HasMetadata replace(StackGresDistributedLogs context, HasMetadata resource) {
    var newSts = safeCast(resource);
    var updatedSts = updateStatefulSet(newSts);
    var pods = getDeployedPods(newSts);
    var podsToUpdate = fixPodAnnotations(newSts, pods);

    podsToUpdate.forEach(podWriter::update);

    return updatedSts;
  }

  public List<Pod> getDeployedPods(StatefulSet sts) {
    final StatefulSetSpec spec = sts.getSpec();
    final Map<String, String> podSelectorLabels = spec.getSelector().getMatchLabels();
    Map<String, String> podLabels = new HashMap<>(podSelectorLabels);
    podLabels.remove(StackGresContext.DISRUPTIBLE_KEY);

    final String namespace = sts.getMetadata().getNamespace();
    return podScanner.findByLabelsAndNamespace(namespace, podLabels)
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public void delete(StackGresDistributedLogs context, HasMetadata resource) {
    final StatefulSet requiredSts = safeCast(resource);
    statefulSetWriter.delete(requiredSts);
  }

  private List<Pod> fixPodAnnotations(StatefulSet requiredSts, List<Pod> pods) {
    var requiredPodAnnotations = Optional.ofNullable(requiredSts
        .getSpec().getTemplate().getMetadata().getAnnotations())
        .map(annotations -> annotations.entrySet().stream()
            .filter(annotation -> !ANNOTATIONS_TO_COMPONENT.containsKey(annotation.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .orElse(null);

    return pods.stream()
        .filter(pod -> !Objects.equals(requiredPodAnnotations, pod.getMetadata().getAnnotations()))
        .peek(pod -> pod.getMetadata().setAnnotations(requiredPodAnnotations))
        .collect(Collectors.toUnmodifiableList());

  }

  private StatefulSet updateStatefulSet(StatefulSet requiredSts) {
    final ObjectMeta metadata = requiredSts.getMetadata();
    statefulSetFinder.findByNameAndNamespace(metadata.getName(), metadata.getNamespace())
        .ifPresent(deployedSts -> requiredSts.getSpec()
            .setVolumeClaimTemplates(deployedSts.getSpec().getVolumeClaimTemplates()));
    return statefulSetWriter.update(requiredSts);
  }

  private StatefulSet updateStatefulSet(StatefulSet requiredSts, StatefulSet deployedSts) {
    requiredSts.getSpec().setVolumeClaimTemplates(deployedSts.getSpec().getVolumeClaimTemplates());
    return statefulSetWriter.update(requiredSts);
  }

}
