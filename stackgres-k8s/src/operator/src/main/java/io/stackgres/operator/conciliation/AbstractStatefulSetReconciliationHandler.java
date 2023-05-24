/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.common.StackGresContext.ANNOTATIONS_TO_COMPONENT;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStatefulSetReconciliationHandler<T extends CustomResource<?, ?>>
    implements ReconciliationHandler<T> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractStatefulSetReconciliationHandler.class);

  public static final Map<String, String> PLACEHOLDER_NODE_SELECTOR =
      Map.of("schedule", "this-pod-is-a-placeholder");

  private final ReconciliationHandler<T> handler;

  private final LabelFactoryForCluster<T> labelFactory;

  private final ResourceFinder<StatefulSet> statefulSetFinder;

  private final ResourceScanner<Pod> podScanner;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final ResourceFinder<Endpoints> endpointsFinder;

  private final ObjectMapper objectMapper;

  protected AbstractStatefulSetReconciliationHandler(
      ReconciliationHandler<T> handler,
      LabelFactoryForCluster<T> labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      ResourceFinder<Endpoints> endpointsFinder,
      ObjectMapper objectMapper) {
    this.handler = handler;
    this.labelFactory = labelFactory;
    this.statefulSetFinder = statefulSetFinder;
    this.podScanner = podScanner;
    this.pvcScanner = pvcScanner;
    this.endpointsFinder = endpointsFinder;
    this.objectMapper = objectMapper;
  }

  public AbstractStatefulSetReconciliationHandler() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.handler = null;
    this.labelFactory = null;
    this.statefulSetFinder = null;
    this.podScanner = null;
    this.pvcScanner = null;
    this.endpointsFinder = null;
    this.objectMapper = null;
  }

  @Override
  public HasMetadata create(T context, HasMetadata resource) {
    return concileSts(context, resource, (c, sts) -> updateStatefulSet(c, sts));
  }

  @Override
  public HasMetadata patch(T context, HasMetadata newResource,
      HasMetadata oldResource) {
    return concileSts(context, newResource, (c, sts) -> updateStatefulSet(c, sts));
  }

  @Override
  public HasMetadata replace(T context, HasMetadata resource) {
    return concileSts(context, resource, (c, sts) -> replaceStatefulSet(c, sts));
  }

  @Override
  public void delete(T context, HasMetadata resource) {
    handler.delete(context, safeCast(resource));
  }

  @Override
  public void deleteWithOrphans(T context, HasMetadata resource) {
    handler.deleteWithOrphans(context, safeCast(resource));
  }

  private StatefulSet safeCast(HasMetadata resource) {
    if (!(resource instanceof StatefulSet)) {
      throw new IllegalArgumentException("Resource must be a StatefulSet instance");
    }
    return (StatefulSet) resource;
  }

  private StatefulSet updateStatefulSet(T context, StatefulSet requiredSts) {
    try {
      return (StatefulSet) handler.patch(context, requiredSts, null);
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 422) {
        return replaceStatefulSet(context, requiredSts);
      } else {
        throw ex;
      }
    }
  }

  private StatefulSet replaceStatefulSet(T context, StatefulSet statefulSet) {
    handler.deleteWithOrphans(context, statefulSet);
    waitStatefulSetToBeDeleted(statefulSet);
    return (StatefulSet) handler.create(context, statefulSet);
  }

  private void waitStatefulSetToBeDeleted(StatefulSet statefulSet) {
    final ObjectMeta metadata = statefulSet.getMetadata();
    waitWithTimeout(
        () -> statefulSetFinder.findByNameAndNamespace(metadata.getName(), metadata.getNamespace())
            .isEmpty(),
        "Timeout while waiting StatefulSet " + statefulSet.getMetadata().getName()
            + " to be deleted");
  }

  private StatefulSet concileSts(T context, HasMetadata resource,
      BiFunction<T, StatefulSet, StatefulSet> writer) {
    final StatefulSet requiredSts = safeCast(resource);
    final StatefulSetSpec spec = requiredSts.getSpec();
    final Map<String, String> appLabel = labelFactory.appLabel();

    final String namespace = resource.getMetadata().getNamespace();

    final int desiredReplicas = spec.getReplicas();
    final int lastReplicaIndex = desiredReplicas - 1;

    var patroniConfigEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.configName(context), namespace);
    final int latestPrimaryIndexFromPatroni =
        PatroniUtil.getLatestPrimaryIndexFromPatroni(patroniConfigEndpoints, objectMapper);
    startPrimaryIfRemoved(context, requiredSts, appLabel, latestPrimaryIndexFromPatroni, writer);

    var pods = findStatefulSetPods(requiredSts, appLabel);
    final boolean existsPodWithPrimaryRole =
        pods.stream().anyMatch(Predicates.and(this::hasRoleLabel, this::isRolePrimary));

    pods.stream()
        .filter(Predicates.or(Predicates.and(this::hasRoleLabel, this::isRolePrimary),
            pod -> !existsPodWithPrimaryRole
                && latestPrimaryIndexFromPatroni == getPodIndex(pod)))
        .filter(pod -> getPodIndex(pod) > lastReplicaIndex)
        .filter(pod -> !isNonDisruptable(context, pod))
        .forEach(pod -> makePrimaryNonDisruptable(context, pod));

    long nonDisruptablePodsRemaining =
        countNonDisruptablePods(context, pods, lastReplicaIndex);
    int replicas = (int) (desiredReplicas - nonDisruptablePodsRemaining);
    spec.setReplicas(replicas);

    final var updatedSts = writer.apply(context, requiredSts);

    removeStatefulSetPlaceholderReplicas(context, requiredSts);

    fixPods(context, requiredSts, updatedSts, appLabel, patroniConfigEndpoints);

    fixPvcs(context, requiredSts, appLabel);

    return updatedSts;
  }

  private void startPrimaryIfRemoved(T context, StatefulSet requiredSts,
      Map<String, String> appLabel, int latestPrimaryIndexFromPatroni,
      BiFunction<T, StatefulSet, StatefulSet> writer) {
    final String namespace = requiredSts.getMetadata().getNamespace();
    final String name = requiredSts.getMetadata().getName();
    if (latestPrimaryIndexFromPatroni <= 0) {
      return;
    }
    var pods = findStatefulSetPods(requiredSts, appLabel);
    if (pods.stream()
        .noneMatch(pod -> getPodIndex(pod) == latestPrimaryIndexFromPatroni)) {
      LOGGER.debug("Detected missing primary Pod that was at index {} for StatefulSet {}.{}",
          latestPrimaryIndexFromPatroni, namespace, name);
      final String podManagementPolicy = requiredSts.getSpec().getPodManagementPolicy();
      final var nodeSelector = requiredSts.getSpec().getTemplate().getSpec().getNodeSelector();
      LOGGER.debug("Create placeholder Pods before primary Pod that was at index {}"
          + " for StatefulSet {}.{}", latestPrimaryIndexFromPatroni, namespace, name);
      requiredSts.getSpec().setPodManagementPolicy("Parallel");
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(PLACEHOLDER_NODE_SELECTOR);
      requiredSts.getSpec().setReplicas(latestPrimaryIndexFromPatroni);
      writer.apply(context, requiredSts);
      waitStatefulSetReplicasToBeCreated(requiredSts);
      LOGGER.debug("Creating primary Pod that was at index {} for StatefulSet {}.{}",
          latestPrimaryIndexFromPatroni, namespace, name);
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelector);
      requiredSts.getSpec().setReplicas(latestPrimaryIndexFromPatroni + 1);
      writer.apply(context, requiredSts);
      waitStatefulSetReplicasToBeCreated(requiredSts);
      requiredSts.getSpec().setPodManagementPolicy(podManagementPolicy);
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelector);
    }
  }

  private void waitStatefulSetReplicasToBeCreated(StatefulSet statefulSet) {
    final String namespace = statefulSet.getMetadata().getNamespace();
    final int stsReplicas = statefulSet.getSpec().getReplicas();
    final Map<String, String> stsMatchLabels = statefulSet.getSpec().getSelector().getMatchLabels();
    waitWithTimeout(
        () -> podScanner.findByLabelsAndNamespace(namespace, stsMatchLabels).size() >= stsReplicas,
        "Timeout while waiting StatefulSet " + statefulSet.getMetadata().getName() + " to reach "
            + stsReplicas + " replicas");
  }

  private void waitWithTimeout(BooleanSupplier supplier, String timeoutMessage) {
    Unchecked.runnable(() -> {
      Instant start = Instant.now();
      while (!supplier.getAsBoolean()) {
        if (Instant.now().isAfter(start.plus(Duration.ofSeconds(5)))) {
          throw new TimeoutException(timeoutMessage);
        }
        TimeUnit.MILLISECONDS.sleep(500);
      }
    }).run();
  }

  private void removeStatefulSetPlaceholderReplicas(T context, StatefulSet statefulSet) {
    final String namespace = statefulSet.getMetadata().getNamespace();
    final Map<String, String> stsMatchLabels = statefulSet.getSpec().getSelector().getMatchLabels();
    podScanner.findByLabelsAndNamespace(namespace, stsMatchLabels).stream()
        .filter(pod -> Objects.equals(PLACEHOLDER_NODE_SELECTOR, pod.getSpec().getNodeSelector()))
        .forEach(pod -> {
          if (LOGGER.isDebugEnabled()) {
            final String podName = pod.getMetadata().getName();
            final String name = statefulSet.getMetadata().getNamespace();
            LOGGER.debug("Removing placeholder Pod {}.{} for StatefulSet {}.{}", namespace, podName,
                namespace, name);
          }
          handler.delete(context, pod);
        });
  }

  private void makePrimaryNonDisruptable(T context, Pod primaryPod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = primaryPod.getMetadata().getNamespace();
      final String podName = primaryPod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Marking primary Pod {}.{} for StatefulSet {}.{} as non disruptible"
          + " since in the last index", namespace, podName, namespace, name);
    }
    final Map<String, String> primaryPodLabels = primaryPod.getMetadata().getLabels();
    primaryPodLabels.put(labelFactory.labelMapper().disruptibleKey(context),
        StackGresContext.WRONG_VALUE);
    handler.patch(context, primaryPod, null);
  }

  private long countNonDisruptablePods(T context, List<Pod> pods,
      int lastReplicaIndex) {
    return pods.stream()
        .filter(pod -> isNonDisruptable(context, pod))
        .map(this::getPodIndex)
        .filter(pod -> pod >= lastReplicaIndex)
        .count();
  }

  private void fixPods(final T context, final StatefulSet statefulSet,
      final StatefulSet deployedStatefulSet, final Map<String, String> appLabel,
      Optional<Endpoints> patroniConfigEndpoints) {
    var podsToFix = findStatefulSetPods(statefulSet, appLabel);
    List<Pod> disruptablePodsToPatch =
        fixNonDisruptablePods(context, statefulSet, patroniConfigEndpoints, podsToFix);
    List<Pod> podAnnotationsToPatch = fixPodsAnnotations(statefulSet, podsToFix);
    List<Pod> podOwnerReferencesToPatch = fixPodsOwnerReferences(
        context, deployedStatefulSet, podsToFix);
    List<Pod> podSelectorMatchLabelsToPatch =
        fixPodsSelectorMatchLabels(context, statefulSet, podsToFix);
    Seq.seq(disruptablePodsToPatch).append(podAnnotationsToPatch)
        .append(podOwnerReferencesToPatch).append(podSelectorMatchLabelsToPatch)
        .grouped(pod -> pod.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pod -> handler.patch(context, pod, null));
  }

  private List<Pod> fixNonDisruptablePods(T context, StatefulSet statefulSet,
      Optional<Endpoints> patroniConfigEndpoints, List<Pod> pods) {
    final int latestPrimaryIndexFromPatroni =
        PatroniUtil.getLatestPrimaryIndexFromPatroni(patroniConfigEndpoints, objectMapper);
    final int replicas = statefulSet.getSpec().getReplicas();
    return pods.stream()
        .filter(pod -> isNonDisruptable(context, pod))
        .filter(this::hasRoleLabel)
        .filter(Predicates.not(this::isRolePrimary))
        .filter(pod -> getPodIndex(pod) != latestPrimaryIndexFromPatroni)
        .filter(pod -> getPodIndex(pod) < replicas)
        .map(pod -> fixNonDisruptablePod(context, pod))
        .toList();
  }

  private Pod fixNonDisruptablePod(T context, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing non disruptable Pod {}.{} for StatefulSet {}.{} as disruptible"
          + " since current or latest primary", namespace, podName, namespace, name);
    }
    pod.getMetadata().getLabels().put(labelFactory.labelMapper().disruptibleKey(context),
        StackGresContext.RIGHT_VALUE);
    return pod;
  }

  private List<Pod> fixPodsAnnotations(StatefulSet statefulSet, List<Pod> pods) {
    var requiredPodAnnotations =
        Optional.ofNullable(statefulSet.getSpec().getTemplate().getMetadata().getAnnotations())
            .map(annotations -> annotations.entrySet().stream()
                .filter(annotation -> !ANNOTATIONS_TO_COMPONENT.containsKey(annotation.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
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
      LOGGER.debug("Fixing annotations for Pod {}.{} for StatefulSet {}.{} to {}",
          namespace, podName, namespace, name, requiredPodAnnotations);
    }
    pod.getMetadata().setAnnotations(Optional.ofNullable(pod.getMetadata().getAnnotations())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(annotation -> requiredPodAnnotations.keySet()
            .stream().noneMatch(annotation.v1::equals))
        .append(Seq.seq(requiredPodAnnotations))
        .toMap(Tuple2::v1, Tuple2::v2));
    return pod;
  }

  private List<Pod> fixPodsOwnerReferences(
      T context, StatefulSet statefulSet, List<Pod> pods) {
    var requiredOwnerReferences = List.of(
        new OwnerReferenceBuilder()
        .withApiVersion(statefulSet.getApiVersion())
        .withKind(statefulSet.getKind())
        .withName(statefulSet.getMetadata().getName())
        .withUid(statefulSet.getMetadata().getUid())
        .withBlockOwnerDeletion(true)
        .withController(true)
        .build());
    var requiredOwnerReferencesForNonDisruptiblePod = List.of(
        ResourceUtil.getOwnerReference(context));

    return Stream.concat(
        pods.stream()
        .filter(pod -> !isNonDisruptable(context, pod))
        .filter(pod -> !Objects.equals(
            requiredOwnerReferences,
            pod.getMetadata().getOwnerReferences()))
        .map(pod -> fixPodOwnerReferences(
            requiredOwnerReferences, pod)),
        pods.stream()
        .filter(pod -> isNonDisruptable(context, pod))
        .filter(pod -> !Objects.equals(
            requiredOwnerReferencesForNonDisruptiblePod,
            pod.getMetadata().getOwnerReferences()))
        .map(pod -> fixPodOwnerReferences(
            requiredOwnerReferencesForNonDisruptiblePod, pod)))
        .toList();
  }

  private Pod fixPodOwnerReferences(List<OwnerReference> requiredOwnerReferences, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing owner references for Pod {}.{} for StatefulSet {}.{} to {}",
          namespace, podName, namespace, name, requiredOwnerReferences);
    }
    pod.getMetadata().setOwnerReferences(requiredOwnerReferences);
    return pod;
  }

  private List<Pod> fixPodsSelectorMatchLabels(final T context, final StatefulSet statefulSet,
      final List<Pod> pods) {
    final var requiredPodLabels =
        Optional.ofNullable(statefulSet.getSpec().getSelector().getMatchLabels())
        .map(labels -> labels.entrySet().stream()
            .filter(label -> !labelFactory.labelMapper().disruptibleKey(context)
                .equals(label.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .orElse(Map.of());
    return pods.stream()
        .filter(pod -> requiredPodLabels.entrySet().stream()
            .anyMatch(requiredPodLabel -> Optional.ofNullable(pod.getMetadata().getLabels())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .noneMatch(podLabel -> Objects.equals(requiredPodLabel, podLabel))))
        .map(pod -> fixPodSelectorMatchLabels(requiredPodLabels, pod))
        .toList();
  }

  private Pod fixPodSelectorMatchLabels(Map<String, String> requiredPodLabels, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing selector match labels for Pod {}.{} for StatefulSet {}.{} to {}",
          namespace, podName, namespace, name, requiredPodLabels);
    }
    pod.getMetadata().setLabels(Optional.ofNullable(pod.getMetadata().getLabels())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(label -> requiredPodLabels.keySet()
            .stream().noneMatch(label.v1::equals))
        .append(Seq.seq(requiredPodLabels))
        .toMap(Tuple2::v1, Tuple2::v2));
    return pod;
  }

  private void fixPvcs(T context, StatefulSet statefulSet, Map<String, String> appLabel) {
    var pods = findStatefulSetPods(statefulSet, appLabel);
    final String namespace = statefulSet.getMetadata().getNamespace();
    var expectedPvcNames = Optional.of(statefulSet)
        .map(StatefulSet::getSpec)
        .map(StatefulSetSpec::getVolumeClaimTemplates)
        .stream()
        .flatMap(List::stream)
        .map(PersistentVolumeClaim::getMetadata)
        .map(ObjectMeta::getName)
        .flatMap(pvcTemplateName -> pods.stream()
            .map(Pod::getMetadata)
            .map(ObjectMeta::getName)
            .map(podName -> pvcTemplateName + "-" + podName))
        .toList();
    var pvcsToFix = pvcScanner.findByLabelsAndNamespace(namespace, appLabel).stream()
        .filter(pvc -> expectedPvcNames.contains(pvc.getMetadata().getName()))
        .toList();
    List<PersistentVolumeClaim> pvcAnnotationsToPatch = fixPvcsAnnotations(
        statefulSet, pvcsToFix);
    List<PersistentVolumeClaim> pvcLabelsToPatch = fixPvcsLabels(
        statefulSet, pvcsToFix);
    Seq.seq(pvcAnnotationsToPatch).append(pvcLabelsToPatch)
        .grouped(pvc -> pvc.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pvc -> handler.patch(context, pvc, null));
  }

  private List<PersistentVolumeClaim> fixPvcsAnnotations(StatefulSet statefulSet,
      List<PersistentVolumeClaim> pvcs) {
    var requiredPvcAnnotations =
        Seq.seq(statefulSet.getSpec().getVolumeClaimTemplates())
        .map(requiredPvc -> Tuple.tuple(requiredPvc.getMetadata().getName(),
            Optional.ofNullable(requiredPvc.getMetadata().getAnnotations())
            .orElse(Map.of())))
        .toList();

    return Seq.seq(pvcs)
        .map(pvc -> Tuple.tuple(pvc, requiredPvcAnnotations.stream()
            .filter(requiredPvcAnnotation -> Optional
                .of(requiredPvcAnnotation.v1 + "-" + statefulSet.getMetadata().getName())
                .filter(pvc.getMetadata().getName()::startsWith)
                .filter(prefix -> ResourceUtil.getIndexPattern().matcher(
                    pvc.getMetadata().getName().substring(prefix.length())).matches())
                .isPresent())
            .map(Tuple2::v2)
            .findFirst()))
        .filter(pvc -> pvc.v2.isPresent())
        .map(pvc -> pvc.map2(Optional::get))
        .filter(pvc -> pvc.v2.entrySet().stream()
            .anyMatch(requiredAnnotation -> Optional
                .ofNullable(pvc.v1.getMetadata().getAnnotations())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .noneMatch(pvcAnnotation -> Objects.equals(requiredAnnotation, pvcAnnotation))))
        .map(pvc -> Tuple.tuple(fixPvcAnnotations(pvc.v2, pvc.v1), pvc.v2))
        .map(Tuple2::v1)
        .toList();
  }

  private PersistentVolumeClaim fixPvcAnnotations(Map<String, String> requiredPvcAnnotations,
      PersistentVolumeClaim pvc) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pvc.getMetadata().getNamespace();
      final String pvcName = pvc.getMetadata().getName();
      final String name = pvcName.substring(0, pvcName.lastIndexOf("-"));
      LOGGER.debug("Fixing annotations for PersistentVolumeClaim {}.{} for StatefulSet {}.{} to {}",
          namespace, pvcName, namespace, name, requiredPvcAnnotations);
    }
    pvc.getMetadata().setAnnotations(Optional.ofNullable(pvc.getMetadata().getAnnotations())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(annotation -> requiredPvcAnnotations.keySet()
            .stream().noneMatch(annotation.v1::equals))
        .append(Seq.seq(requiredPvcAnnotations))
        .toMap(Tuple2::v1, Tuple2::v2));
    return pvc;
  }

  private List<PersistentVolumeClaim> fixPvcsLabels(StatefulSet statefulSet,
      List<PersistentVolumeClaim> pvcs) {
    var requiredPvcLabels =
        Seq.seq(statefulSet.getSpec().getVolumeClaimTemplates())
        .map(requiredPvc -> Tuple.tuple(requiredPvc.getMetadata().getName(),
            Optional.ofNullable(requiredPvc.getMetadata().getLabels())
            .orElse(Map.of())))
        .toList();

    return Seq.seq(pvcs)
        .map(pvc -> Tuple.tuple(pvc, requiredPvcLabels.stream()
            .filter(requiredPvcLabel -> Optional
                .of(requiredPvcLabel.v1 + "-" + statefulSet.getMetadata().getName())
                .filter(pvc.getMetadata().getName()::startsWith)
                .filter(prefix -> ResourceUtil.getIndexPattern().matcher(
                    pvc.getMetadata().getName().substring(prefix.length())).matches())
                .isPresent())
            .map(Tuple2::v2)
            .findFirst()))
        .filter(pvc -> pvc.v2.isPresent())
        .map(pvc -> pvc.map2(Optional::get))
        .filter(pvc -> pvc.v2.entrySet().stream()
            .anyMatch(requiredLabel -> Optional
                .ofNullable(pvc.v1.getMetadata().getLabels())
                .stream()
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .noneMatch(pvcLabel -> Objects.equals(requiredLabel, pvcLabel))))
        .map(pvc -> Tuple.tuple(fixPvcLabels(pvc.v2, pvc.v1), pvc.v2))
        .map(Tuple2::v1)
        .toList();
  }

  private PersistentVolumeClaim fixPvcLabels(Map<String, String> requiredPvcLabels,
      PersistentVolumeClaim pvc) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pvc.getMetadata().getNamespace();
      final String pvcName = pvc.getMetadata().getName();
      final String name = pvcName.substring(0, pvcName.lastIndexOf("-"));
      LOGGER.debug("Fixing labels for PersistentVolumeClaim {}.{} for StatefulSet {}.{}"
          + " to {}", namespace, pvcName, namespace, name, requiredPvcLabels);
    }
    pvc.getMetadata().setLabels(Optional.ofNullable(pvc.getMetadata().getLabels())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(label -> requiredPvcLabels.keySet()
            .stream().noneMatch(label.v1::equals))
        .append(Seq.seq(requiredPvcLabels))
        .toMap(Tuple2::v1, Tuple2::v2));
    return pvc;
  }

  private List<Pod> findStatefulSetPods(final StatefulSet updatedSts,
      final Map<String, String> appLabel) {
    final String namespace = updatedSts.getMetadata().getNamespace();
    final String name = updatedSts.getMetadata().getName();
    var stsPodNameMatcher = Pattern.compile("^" + name + "-[0-9]+$");
    return podScanner.findByLabelsAndNamespace(namespace, appLabel).stream()
        .filter(pod -> Optional.of(pod)
            .map(Pod::getMetadata)
            .map(ObjectMeta::getName)
            .map(stsPodNameMatcher::matcher)
            .filter(Matcher::matches)
            .isPresent())
        .sorted(Comparator.comparing(this::getPodIndex))
        .toList();
  }

  private boolean hasRoleLabel(Pod pod) {
    return pod.getMetadata().getLabels().containsKey(PatroniUtil.ROLE_KEY);
  }

  private boolean isRolePrimary(Pod pod) {
    return Objects.equals(
        pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY),
        PatroniUtil.PRIMARY_ROLE);
  }

  private boolean isNonDisruptable(T context, Pod pod) {
    return Objects.equals(
        pod.getMetadata().getLabels().get(labelFactory.labelMapper().disruptibleKey(context)),
        StackGresContext.WRONG_VALUE);
  }

  private int getPodIndex(Pod pod) {
    return ResourceUtil.getIndexPattern()
        .matcher(pod.getMetadata().getName())
        .results()
        .findFirst()
        .map(result -> result.group(1))
        .map(Integer::parseInt)
        .orElseThrow();
  }

}
