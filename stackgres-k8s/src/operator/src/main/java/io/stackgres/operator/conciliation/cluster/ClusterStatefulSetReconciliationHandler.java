/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static io.stackgres.common.StackGresContext.ANNOTATIONS_TO_COMPONENT;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.DeployedResourceDecorator;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ReconciliationScope(value = StackGresCluster.class, kind = "StatefulSet")
@ApplicationScoped
public class ClusterStatefulSetReconciliationHandler
    implements ReconciliationHandler<StackGresCluster>, DeployedResourceDecorator {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(ClusterStatefulSetReconciliationHandler.class);

  static final ImmutableMap<String, String> PLACEHOLDER_NODE_SELECTOR =
      ImmutableMap.of("schedule", "this-pod-is-a-placeholder");

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  private final ResourceFinder<StatefulSet> statefulSetFinder;

  private final ResourceWriter<StatefulSet> statefulSetWriter;

  private final ResourceScanner<Pod> podScanner;

  private final ResourceWriter<Pod> podWriter;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final ResourceWriter<PersistentVolumeClaim> pvcWriter;

  private final ResourceFinder<Endpoints> endpointsFinder;

  private final JsonMapper objectMapper;

  @Inject
  public ClusterStatefulSetReconciliationHandler(
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceWriter<StatefulSet> statefulSetWriter,
      ResourceScanner<Pod> podScanner,
      ResourceWriter<Pod> podWriter,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      ResourceWriter<PersistentVolumeClaim> pvcWriter,
      ResourceFinder<Endpoints> endpointsFinder, JsonMapper objectMapper) {
    this.labelFactory = labelFactory;
    this.statefulSetFinder = statefulSetFinder;
    this.statefulSetWriter = statefulSetWriter;
    this.podScanner = podScanner;
    this.podWriter = podWriter;
    this.pvcScanner = pvcScanner;
    this.pvcWriter = pvcWriter;
    this.endpointsFinder = endpointsFinder;
    this.objectMapper = objectMapper;
  }

  @Override
  public HasMetadata create(StackGresCluster context, HasMetadata resource) {
    return concileSts(context, resource, this::updateStatefulSet);
  }

  @Override
  public HasMetadata patch(StackGresCluster context, HasMetadata newResource,
      HasMetadata oldResource) {
    return concileSts(context, newResource, this::updateStatefulSet);
  }

  @Override
  public HasMetadata replace(StackGresCluster context, HasMetadata resource) {
    return concileSts(context, resource, this::replaceStatefulSet);
  }

  @Override
  public void delete(StackGresCluster context, HasMetadata resource) {
    statefulSetWriter.delete(safeCast(resource));
  }

  private StatefulSet safeCast(HasMetadata resource) {
    if (!(resource instanceof StatefulSet)) {
      throw new IllegalArgumentException("Resource must be a StatefulSet instance");
    }
    return (StatefulSet) resource;
  }

  @Override
  public void decorate(HasMetadata resource) {
    StatefulSet sts = safeCast(resource);

    int actualReplicas = sts.getSpec().getReplicas();
    Map<String, String> stsSelectorLabels = sts.getSpec().getSelector().getMatchLabels();
    Map<String, String> nonDisruptingLabels = new HashMap<>(stsSelectorLabels);
    nonDisruptingLabels.put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.WRONG_VALUE);
    var pods =
        podScanner.findByLabelsAndNamespace(sts.getMetadata().getNamespace(), nonDisruptingLabels);
    actualReplicas += pods.size();
    sts.getSpec().setReplicas(actualReplicas);
  }

  private StatefulSet updateStatefulSet(StatefulSet requiredSts) {
    try {
      return statefulSetWriter.update(requiredSts);
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 422) {
        return replaceStatefulSet(requiredSts);
      } else {
        throw ex;
      }
    }
  }

  private StatefulSet replaceStatefulSet(StatefulSet requiredSts) {
    statefulSetWriter.deleteWithoutCascading(requiredSts);
    waitStatefulSetToBeDeleted(requiredSts);
    return statefulSetWriter.create(requiredSts);
  }

  private void waitStatefulSetToBeDeleted(StatefulSet requiredSts) {
    final ObjectMeta metadata = requiredSts.getMetadata();
    waitWithTimeout(
        () -> statefulSetFinder.findByNameAndNamespace(metadata.getName(), metadata.getNamespace())
            .isEmpty(),
        "Timeout while waiting StatefulSet " + requiredSts.getMetadata().getName()
            + " to be deleted");
  }

  private StatefulSet concileSts(StackGresCluster context, HasMetadata resource,
      Function<StatefulSet, StatefulSet> writer) {
    final StatefulSet requiredSts = safeCast(resource);
    final StatefulSetSpec spec = requiredSts.getSpec();
    final Map<String, String> patroniClusterLabels = labelFactory.patroniClusterLabels(context);
    final Map<String, String> labels = labelFactory.clusterLabels(context);

    final String namespace = resource.getMetadata().getNamespace();

    final int desiredReplicas = spec.getReplicas();
    final int lastReplicaIndex = desiredReplicas - 1;

    var patroniConfigEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.configName(context), namespace);
    final int latestPrimaryIndexFromPatroni =
        PatroniUtil.getLatestPrimaryIndexFromPatroni(patroniConfigEndpoints, objectMapper);
    startPrimaryIfRemoved(context, requiredSts, latestPrimaryIndexFromPatroni, writer);

    var pods = podScanner.findByLabelsAndNamespace(namespace, patroniClusterLabels).stream()
        .sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());
    final boolean existsPodWithPrimaryRole =
        pods.stream().anyMatch(Predicates.and(this::hasRoleLabel, this::isRolePrimary));
    final StatefulSet updatedSts;
    if (existsPodWithPrimaryRole || latestPrimaryIndexFromPatroni <= 0) {
      pods.stream()
          .filter(Predicates.or(Predicates.and(this::hasRoleLabel, this::isRolePrimary),
              pod -> !existsPodWithPrimaryRole
                  && latestPrimaryIndexFromPatroni == getPodIndex(pod)))
          .filter(pod -> getPodIndex(pod) > lastReplicaIndex)
          .filter(Predicates.not(this::isNonDisruptable)).forEach(this::makePrimaryNonDisruptable);

      long nonDisruptablePodsRemaining =
          countNonDisruptablePods(pods, lastReplicaIndex);
      int replicas = (int) (desiredReplicas - nonDisruptablePodsRemaining);
      spec.setReplicas(replicas);

      updatedSts = writer.apply(requiredSts);

      removeStatefulSetPlaceholderReplicas(requiredSts);
    } else {
      updatedSts = statefulSetFinder
          .findByNameAndNamespace(requiredSts.getMetadata().getName(), namespace).orElseThrow();
    }

    fixPods(updatedSts, requiredSts, patroniClusterLabels, namespace, patroniConfigEndpoints);

    fixPvcs(updatedSts, requiredSts, labels, namespace);

    return updatedSts;
  }

  private void startPrimaryIfRemoved(StackGresCluster context, StatefulSet requiredSts,
      int latestPrimaryIndexFromPatroni, Function<StatefulSet, StatefulSet> updater) {
    final String namespace = requiredSts.getMetadata().getNamespace();
    final String name = requiredSts.getMetadata().getName();
    final Map<String, String> patroniClusterLabels = labelFactory.patroniClusterLabels(context);
    if (latestPrimaryIndexFromPatroni <= 0) {
      return;
    }
    var pods = podScanner.findByLabelsAndNamespace(namespace, patroniClusterLabels);
    if (pods.stream()
        .noneMatch(pod -> getPodIndex(pod) == latestPrimaryIndexFromPatroni)) {
      LOGGER.debug("Detected missing primary Pod that was at index {} for StatefulSet {}.{}",
          latestPrimaryIndexFromPatroni, namespace, name);
      final String podManagementPolicy = requiredSts.getSpec().getPodManagementPolicy();
      final var nodeSelector = requiredSts.getSpec().getTemplate().getSpec().getNodeSelector();
      final int replicas = requiredSts.getSpec().getReplicas();
      LOGGER.debug("Create placeholder Pods before primary Pod that was at index {}"
          + " for StatefulSet {}.{}", latestPrimaryIndexFromPatroni, namespace, name);
      requiredSts.getSpec().setPodManagementPolicy("Parallel");
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(PLACEHOLDER_NODE_SELECTOR);
      requiredSts.getSpec().setReplicas(latestPrimaryIndexFromPatroni);
      updater.apply(requiredSts);
      waitStatefulSetReplicasToBeCreated(requiredSts);
      LOGGER.debug("Creating primary Pod that was at index {} for StatefulSet {}.{}",
          latestPrimaryIndexFromPatroni, namespace, name);
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelector);
      requiredSts.getSpec().setReplicas(latestPrimaryIndexFromPatroni + 1);
      updater.apply(requiredSts);
      waitStatefulSetReplicasToBeCreated(requiredSts);
      requiredSts.getSpec().setPodManagementPolicy(podManagementPolicy);
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelector);
      requiredSts.getSpec().setReplicas(replicas);
    }
  }

  private void waitStatefulSetReplicasToBeCreated(StatefulSet requiredSts) {
    final String namespace = requiredSts.getMetadata().getNamespace();
    final int stsReplicas = requiredSts.getSpec().getReplicas();
    final Map<String, String> stsMatchLabels = requiredSts.getSpec().getSelector().getMatchLabels();
    waitWithTimeout(
        () -> podScanner.findByLabelsAndNamespace(namespace, stsMatchLabels).size() >= stsReplicas,
        "Timeout while waiting StatefulSet " + requiredSts.getMetadata().getName() + " to reach "
            + stsReplicas + " replicas");
  }

  private void waitWithTimeout(Supplier<Boolean> supplier, String timeoutMessage) {
    Unchecked.runnable(() -> {
      Instant start = Instant.now();
      while (!supplier.get()) {
        if (Instant.now().isAfter(start.plus(Duration.ofSeconds(5)))) {
          throw new TimeoutException(timeoutMessage);
        }
        TimeUnit.MILLISECONDS.sleep(500);
      }
    }).run();
  }

  private void removeStatefulSetPlaceholderReplicas(StatefulSet requiredSts) {
    final String namespace = requiredSts.getMetadata().getNamespace();
    final Map<String, String> stsMatchLabels = requiredSts.getSpec().getSelector().getMatchLabels();
    podScanner.findByLabelsAndNamespace(namespace, stsMatchLabels).stream()
        .filter(pod -> Objects.equals(PLACEHOLDER_NODE_SELECTOR, pod.getSpec().getNodeSelector()))
        .forEach(pod -> {
          if (LOGGER.isDebugEnabled()) {
            final String podName = pod.getMetadata().getName();
            final String name = requiredSts.getMetadata().getNamespace();
            LOGGER.debug("Removing placeholder Pod {}.{} for StatefulSet {}.{}", namespace, podName,
                namespace, name);
          }
          podWriter.delete(pod);
        });
  }

  private void makePrimaryNonDisruptable(Pod primaryPod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = primaryPod.getMetadata().getNamespace();
      final String podName = primaryPod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Marking primary Pod {}.{} for StatefulSet {}.{} as non disruptible"
          + " since in the last index", namespace, podName, namespace, name);
    }
    final Map<String, String> primaryPodLabels = primaryPod.getMetadata().getLabels();
    primaryPodLabels.put(StackGresContext.DISRUPTIBLE_KEY, StackGresContext.WRONG_VALUE);
    podWriter.update(primaryPod);
  }

  private long countNonDisruptablePods(List<Pod> pods,
      int lastReplicaIndex) {
    return pods.stream().filter(this::isNonDisruptable).map(pod -> getPodIndex(pod))
        .filter(pod -> pod >= lastReplicaIndex).count();
  }

  private void fixPods(final StatefulSet updatedSts, final StatefulSet requiredSts,
      final Map<String, String> patroniClusterLabels, final String namespace,
      @NotNull Optional<Endpoints> patroniConfigEndpoints) {
    var podsToFix = podScanner.findByLabelsAndNamespace(namespace, patroniClusterLabels).stream()
        .sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());
    List<Pod> disruptablePodsToPatch =
        fixNonDisruptablePods(patroniConfigEndpoints, podsToFix);
    List<Pod> podAnnotationsToPatch = fixPodsAnnotations(requiredSts, podsToFix);
    List<Pod> podOwnerReferencesToPatch = fixPodsOwnerReferences(updatedSts, podsToFix);
    Seq.concat(disruptablePodsToPatch.stream(), podAnnotationsToPatch.stream(),
        podOwnerReferencesToPatch.stream())
        .grouped(pod -> pod.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(podWriter::update);
  }

  private List<Pod> fixNonDisruptablePods(
      Optional<Endpoints> patroniConfigEndpoints, List<Pod> pods) {
    final int latestPrimaryIndexFromPatroni =
        PatroniUtil.getLatestPrimaryIndexFromPatroni(patroniConfigEndpoints, objectMapper);
    return pods.stream().filter(this::isNonDisruptable).filter(this::hasRoleLabel)
        .filter(Predicates.not(this::isRolePrimary))
        .filter(pod -> getPodIndex(pod) != latestPrimaryIndexFromPatroni)
        .peek(this::fixNonDisruptablePod).collect(ImmutableList.toImmutableList());
  }

  private String fixNonDisruptablePod(Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing non disruptable Pod {}.{} for StatefulSet {}.{} as disruptible"
          + " since current or latest primary", namespace, podName, namespace, name);
    }
    return pod.getMetadata().getLabels().put(StackGresContext.DISRUPTIBLE_KEY,
        StackGresContext.RIGHT_VALUE);
  }

  private List<Pod> fixPodsAnnotations(StatefulSet requiredSts, List<Pod> pods) {
    var requiredPodAnnotations =
        Optional.ofNullable(requiredSts.getSpec().getTemplate().getMetadata().getAnnotations())
            .map(annotations -> annotations.entrySet().stream()
                .filter(annotation -> !ANNOTATIONS_TO_COMPONENT.containsKey(annotation.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(ImmutableMap.of());

    return pods.stream()
        .filter(pod -> requiredPodAnnotations.entrySet().stream()
            .anyMatch(requiredAnnotation -> Optional.ofNullable(pod.getMetadata().getAnnotations())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .anyMatch(podAnnotation -> Objects.equals(requiredAnnotation, podAnnotation))))
        .peek(pod -> fixPodAnnotations(requiredPodAnnotations, pod))
        .collect(ImmutableList.toImmutableList());
  }

  private void fixPodAnnotations(Map<String, String> requiredPodAnnotations, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing annotations for Pod {}.{} for StatefulSet {}.{}"
          + " to {}", namespace, podName, namespace, name, requiredPodAnnotations);
    }
    pod.getMetadata().setAnnotations(ImmutableMap.<String, String>builder()
        .putAll(Optional.ofNullable(pod.getMetadata().getAnnotations())
            .stream()
            .map(Map::entrySet)
            .flatMap(Set::stream)
            .filter(podAnnotation -> requiredPodAnnotations
                .keySet()
                .stream()
                .noneMatch(podAnnotation.getKey()::equals))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
        .putAll(requiredPodAnnotations)
        .build());
  }

  private List<Pod> fixPodsOwnerReferences(StatefulSet updatedSts, List<Pod> pods) {
    var requiredOwnerReferences = ImmutableList.of(new OwnerReferenceBuilder()
        .withApiVersion(updatedSts.getApiVersion())
        .withKind(updatedSts.getKind())
        .withName(updatedSts.getMetadata().getName())
        .withUid(updatedSts.getMetadata().getUid())
        .withBlockOwnerDeletion(true)
        .withController(true)
        .build());

    return pods.stream()
        .filter(pod -> !Objects.equals(
            requiredOwnerReferences, pod.getMetadata().getOwnerReferences()))
        .peek(pod -> fixPodOwnerReferences(requiredOwnerReferences, pod))
        .collect(ImmutableList.toImmutableList());
  }

  private void fixPodOwnerReferences(List<OwnerReference> requiredOwnerReferences, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing owner references for Pod {}.{} for StatefulSet {}.{}"
          + " to {}", namespace, podName, namespace, name, requiredOwnerReferences);
    }
    pod.getMetadata().setOwnerReferences(requiredOwnerReferences);
  }

  private void fixPvcs(final StatefulSet updatedSts, final StatefulSet requiredSts,
      final Map<String, String> labels, final String namespace) {
    var pvcsToFix = pvcScanner.findByLabelsAndNamespace(namespace, labels).stream()
        .collect(Collectors.toUnmodifiableList());
    List<PersistentVolumeClaim> pvcAnnotationsToPatch = fixPvcsAnnotations(
        requiredSts, pvcsToFix);
    List<PersistentVolumeClaim> pvcOwnerReferencesToPatch = fixPvcsOwnerReferences(
        updatedSts, pvcsToFix);
    Seq.concat(pvcAnnotationsToPatch.stream(), pvcOwnerReferencesToPatch.stream())
        .grouped(pod -> pod.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pvcWriter::update);
  }

  private List<PersistentVolumeClaim> fixPvcsAnnotations(StatefulSet requiredSts,
      List<PersistentVolumeClaim> pvcs) {
    var requiredPvcAnnotations =
        Seq.seq(requiredSts.getSpec().getVolumeClaimTemplates())
        .map(requiredPvc -> Tuple.tuple(requiredPvc.getMetadata().getName(),
            Optional.ofNullable(requiredPvc.getMetadata().getAnnotations())
            .orElse(ImmutableMap.of())))
        .toList();

    return pvcs.stream()
        .map(pvc -> Tuple.tuple(requiredPvcAnnotations
            .stream()
            .filter(requiredPvcAnnotation -> Objects.equals(requiredPvcAnnotation.v1,
                pvc.getMetadata().getName()))
            .map(Tuple2::v2)
            .findFirst(), pvc))
        .filter(pvc -> pvc.v1.isPresent())
        .map(pvc -> pvc.map1(Optional::get))
        .filter(pvc -> pvc.v1.entrySet().stream()
            .anyMatch(requiredAnnotation -> Optional
                .ofNullable(pvc.v2.getMetadata().getAnnotations())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .anyMatch(podAnnotation -> Objects.equals(requiredAnnotation, podAnnotation))))
        .peek(pvc -> fixPvcAnnotations(pvc.v1, pvc.v2))
        .map(Tuple2::v2)
        .collect(ImmutableList.toImmutableList());
  }

  private void fixPvcAnnotations(Map<String, String> requiredPvcAnnotations,
      PersistentVolumeClaim pvc) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pvc.getMetadata().getNamespace();
      final String pvcName = pvc.getMetadata().getName();
      final String name = pvcName.substring(0, pvcName.lastIndexOf("-"));
      LOGGER.debug("Fixing annotations for PersistentVolumeClaim {}.{} for StatefulSet {}.{}"
          + " to {}", namespace, pvcName, namespace, name, requiredPvcAnnotations);
    }
    pvc.getMetadata().setAnnotations(ImmutableMap.<String, String>builder()
        .putAll(Optional.ofNullable(pvc.getMetadata().getAnnotations())
            .stream()
            .map(Map::entrySet)
            .flatMap(Set::stream)
            .filter(podAnnotation -> requiredPvcAnnotations
                .keySet()
                .stream()
                .noneMatch(podAnnotation.getKey()::equals))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
        .putAll(requiredPvcAnnotations)
        .build());
  }

  private List<PersistentVolumeClaim> fixPvcsOwnerReferences(StatefulSet updatedSts,
      List<PersistentVolumeClaim> pvcs) {
    var requiredOwnerReferences = ImmutableList.of(new OwnerReferenceBuilder()
        .withApiVersion(updatedSts.getApiVersion())
        .withKind(updatedSts.getKind())
        .withName(updatedSts.getMetadata().getName())
        .withUid(updatedSts.getMetadata().getUid())
        .withBlockOwnerDeletion(true)
        .withController(true)
        .build());

    return pvcs.stream()
        .filter(pvc -> !Objects.equals(
            requiredOwnerReferences, pvc.getMetadata().getOwnerReferences()))
        .peek(pvc -> fixPvcOwnerReferences(requiredOwnerReferences, pvc))
        .collect(ImmutableList.toImmutableList());
  }

  private void fixPvcOwnerReferences(List<OwnerReference> requiredOwnerReferences,
      PersistentVolumeClaim pvc) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pvc.getMetadata().getNamespace();
      final String pvcName = pvc.getMetadata().getName();
      final String name = pvcName.substring(0, pvcName.lastIndexOf("-"));
      LOGGER.debug("Fixing owner references for PersistentVolumeClaim {}.{} for StatefulSet {}.{}"
          + " to {}", namespace, pvcName, namespace, name, requiredOwnerReferences);
    }
    pvc.getMetadata().setOwnerReferences(requiredOwnerReferences);
  }

  private boolean hasRoleLabel(Pod pod) {
    return pod.getMetadata().getLabels().containsKey(PatroniUtil.ROLE_KEY);
  }

  private boolean isRolePrimary(Pod pod) {
    return pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)
        .equals(PatroniUtil.PRIMARY_ROLE);
  }

  private boolean isNonDisruptable(Pod pod) {
    return pod.getMetadata().getLabels().get(StackGresContext.DISRUPTIBLE_KEY)
        .equals(StackGresContext.WRONG_VALUE);
  }

  private int getPodIndex(Pod pod) {
    return Integer.parseInt(ResourceUtil.getIndexPattern().matcher(pod.getMetadata().getName())
        .results().findFirst().orElseThrow().group(1));
  }

}
