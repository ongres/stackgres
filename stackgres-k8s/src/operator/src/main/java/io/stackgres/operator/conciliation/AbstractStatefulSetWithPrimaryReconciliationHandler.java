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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniCtl.PatroniCtlInstance;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStatefulSetWithPrimaryReconciliationHandler<T extends CustomResource<?, ?>>
    implements ReconciliationHandler<T> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractStatefulSetWithPrimaryReconciliationHandler.class);

  public static final Map<String, String> PLACEHOLDER_NODE_SELECTOR =
      Map.of("schedule", "this-pod-is-a-placeholder");

  private final ReconciliationHandler<T> handler;

  private final ReconciliationHandler<T> protectHandler;

  private final LabelFactoryForCluster<T> labelFactory;

  private final ResourceFinder<StatefulSet> statefulSetFinder;

  private final ResourceScanner<Pod> podScanner;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final PatroniCtl patroniCtl;

  private final ObjectMapper objectMapper;

  protected AbstractStatefulSetWithPrimaryReconciliationHandler(
      ReconciliationHandler<T> handler,
      ReconciliationHandler<T> protectHandler,
      LabelFactoryForCluster<T> labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      PatroniCtl patroniCtl,
      ObjectMapper objectMapper) {
    this.handler = handler;
    this.protectHandler = protectHandler;
    this.labelFactory = labelFactory;
    this.statefulSetFinder = statefulSetFinder;
    this.podScanner = podScanner;
    this.pvcScanner = pvcScanner;
    this.patroniCtl = patroniCtl;
    this.objectMapper = objectMapper;
  }

  public AbstractStatefulSetWithPrimaryReconciliationHandler() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.handler = null;
    this.protectHandler = null;
    this.labelFactory = null;
    this.statefulSetFinder = null;
    this.podScanner = null;
    this.pvcScanner = null;
    this.patroniCtl = null;
    this.objectMapper = null;
  }

  protected abstract boolean isPatroniOnKubernetes(T context);

  @Override
  public HasMetadata create(T context, HasMetadata resource) {
    return concileSts(context, resource, (c, sts) -> createStatefulSet(c, sts));
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

  private StatefulSet createStatefulSet(T context, StatefulSet requiredSts) {
    return (StatefulSet) handler.create(context, requiredSts);
  }

  private StatefulSet updateStatefulSet(T context, StatefulSet requiredSts) {
    try {
      return (StatefulSet) handler.patch(context, requiredSts, null);
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 422) {
        final Map<String, String> appLabel = labelFactory.appLabel();
        var deployedStatefulSet = statefulSetFinder.findByNameAndNamespace(
            requiredSts.getMetadata().getName(),
            requiredSts.getMetadata().getNamespace())
            .orElseThrow(() -> new RuntimeException(
                HasMetadata.getKind(context.getClass()) + " "
                + requiredSts.getMetadata().getNamespace()
                + "." + requiredSts.getMetadata().getName()
                + " not fount while replacing it"));

        protectPodsFromStatefulSetRemoval(context, deployedStatefulSet, appLabel);

        protectPvcsFromStatefulSetRemoval(context, deployedStatefulSet, appLabel);

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

  private StatefulSet concileSts(
      T context,
      HasMetadata resource,
      BiFunction<T, StatefulSet, StatefulSet> writer) {
    final StatefulSet requiredSts;
    try {
      requiredSts = objectMapper.treeToValue(
          objectMapper.valueToTree(safeCast(resource)), StatefulSet.class);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
    final StatefulSetSpec spec = requiredSts.getSpec();
    final Map<String, String> appLabel = labelFactory.appLabel();

    final int desiredReplicas = spec.getReplicas();
    final int lastReplicaIndex = desiredReplicas - 1;

    final var patroniCtl = this.patroniCtl.instanceFor(context);
    final Optional<String> latestPrimaryFromPatroni =
        PatroniUtil.getLatestPrimaryFromPatroni(patroniCtl);
    if (desiredReplicas > 0) {
      startPrimaryIfRemoved(context, requiredSts, appLabel, latestPrimaryFromPatroni, writer);
    }

    var pods = findStatefulSetPods(requiredSts, appLabel);
    if (desiredReplicas > 0) {
      pods.stream()
          .filter(pod -> latestPrimaryFromPatroni.map(pod.getMetadata().getName()::equals).orElse(false))
          .filter(pod -> getPodIndex(pod) > lastReplicaIndex)
          .filter(pod -> !isNonDisruptable(context, pod))
          .forEach(pod -> makePrimaryPodNonDisruptable(context, pod));
      long nonDisruptablePodsRemaining =
          countNonDisruptablePods(context, pods, lastReplicaIndex);
      int replicas = Math.max(0, (int) (desiredReplicas - nonDisruptablePodsRemaining));
      spec.setReplicas(replicas);
    } else {
      pods.stream()
          .filter(pod -> isNonDisruptable(context, pod))
          .forEach(pod -> makePrimaryPodDisruptable(context, pod));
      spec.setReplicas(0);
    }

    final var updatedSts = writer.apply(context, requiredSts);

    removeStatefulSetPlaceholderReplicas(context, requiredSts);

    fixPods(context, requiredSts, updatedSts, appLabel, patroniCtl);

    fixPvcs(context, requiredSts, updatedSts, appLabel);

    return updatedSts;
  }

  private void startPrimaryIfRemoved(T context, StatefulSet requiredSts,
      Map<String, String> appLabel, Optional<String> latestPrimaryFromPatroni,
      BiFunction<T, StatefulSet, StatefulSet> writer) {
    final String namespace = requiredSts.getMetadata().getNamespace();
    final String name = requiredSts.getMetadata().getName();
    if (latestPrimaryFromPatroni.map(ResourceUtil::getIndexFromNameWithIndex).orElse(0) <= 0) {
      return;
    }
    var pods = findStatefulSetPods(requiredSts, appLabel);
    if (latestPrimaryFromPatroni
        .map(ResourceUtil.getNameWithIndexPattern(name)::matcher)
        .map(Matcher::find)
        .orElse(false)
        && pods.stream()
        .noneMatch(pod -> latestPrimaryFromPatroni.map(pod.getMetadata().getName()::equals).orElse(false))) {
      LOGGER.debug("Detected missing primary Pod {} for StatefulSet {}.{}",
          latestPrimaryFromPatroni, namespace, name);
      final String podManagementPolicy = requiredSts.getSpec().getPodManagementPolicy();
      final var nodeSelector = requiredSts.getSpec().getTemplate().getSpec().getNodeSelector();
      LOGGER.debug("Create placeholder Pods before primary Pod that was at index {}"
          + " for StatefulSet {}.{}", latestPrimaryFromPatroni, namespace, name);
      requiredSts.getSpec().setPodManagementPolicy("Parallel");
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(PLACEHOLDER_NODE_SELECTOR);
      requiredSts.getSpec().setReplicas(
          latestPrimaryFromPatroni.map(ResourceUtil::getIndexFromNameWithIndex).orElse(0));
      writer.apply(context, requiredSts);
      waitStatefulSetReplicasToBeCreated(requiredSts);
      LOGGER.debug("Creating primary Pod that was {} for StatefulSet {}.{}",
          latestPrimaryFromPatroni, namespace, name);
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelector);
      requiredSts.getSpec().setReplicas(
          latestPrimaryFromPatroni.map(ResourceUtil::getIndexFromNameWithIndex).orElse(0) + 1);
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
        () -> podScanner.getResourcesInNamespaceWithLabels(namespace, stsMatchLabels).size() >= stsReplicas,
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
    podScanner.getResourcesInNamespaceWithLabels(namespace, stsMatchLabels).stream()
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

  private void makePrimaryPodNonDisruptable(T context, Pod primaryPod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = primaryPod.getMetadata().getNamespace();
      final String podName = primaryPod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Marking primary Pod {}.{} for StatefulSet {}.{} as non disruptible"
          + " since in the last index", namespace, podName, namespace, name);
    }
    final Map<String, String> primaryPodLabels = primaryPod.getMetadata().getLabels();
    primaryPodLabels.put(labelFactory.labelMapper().disruptableKey(context),
        StackGresContext.WRONG_VALUE);
    handler.patch(context, primaryPod, null);
  }

  private void makePrimaryPodDisruptable(T context, Pod primaryPod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = primaryPod.getMetadata().getNamespace();
      final String podName = primaryPod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Marking primary Pod {}.{} for StatefulSet {}.{} as disruptible"
          + " since 0 desired replicas", namespace, podName, namespace, name);
    }
    final Map<String, String> primaryPodLabels = primaryPod.getMetadata().getLabels();
    primaryPodLabels.put(labelFactory.labelMapper().disruptableKey(context),
        StackGresContext.RIGHT_VALUE);
    handler.patch(context, primaryPod, null);
  }

  private long countNonDisruptablePods(
      T context,
      List<Pod> pods,
      int lastReplicaIndex) {
    return pods.stream()
        .filter(pod -> isNonDisruptable(context, pod))
        .map(this::getPodIndex)
        .filter(pod -> pod > lastReplicaIndex)
        .count();
  }

  private void protectPodsFromStatefulSetRemoval(
      final T context,
      final StatefulSet deployedStatefulSet,
      final Map<String, String> appLabel) {
    var podsToProtect = findStatefulSetPods(deployedStatefulSet, appLabel);
    var requiredOwnerReferences = List.of(
        new OwnerReferenceBuilder()
        .withApiVersion(deployedStatefulSet.getApiVersion())
        .withKind(deployedStatefulSet.getKind())
        .withName(deployedStatefulSet.getMetadata().getName())
        .withUid(deployedStatefulSet.getMetadata().getUid())
        .withBlockOwnerDeletion(true)
        .withController(true)
        .build(),
        ResourceUtil.getOwnerReference(context));

    Seq.seq(podsToProtect)
        .filter(pod -> !Objects.equals(
            pod.getMetadata().getOwnerReferences(),
            requiredOwnerReferences))
        .map(pod -> fixPodOwnerReferences(
            requiredOwnerReferences, pod,
            deployedStatefulSet.getMetadata().getName()))
        .grouped(pod -> pod.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pod -> protectHandler.patch(context, pod, null));
  }

  private void protectPvcsFromStatefulSetRemoval(
      T context,
      StatefulSet deployedStatefulSet,
      Map<String, String> appLabel) {
    final String namespace = deployedStatefulSet.getMetadata().getNamespace();
    Pattern statefulSetPodDataPersistentVolumeClaimPattern = ResourceUtil.getNameWithIndexPattern(
        StackGresUtil.statefulSetPodDataPersistentVolumeClaimName(context));
    var pvcsToProtect = pvcScanner.getResourcesInNamespaceWithLabels(namespace, appLabel).stream()
        .filter(pvc -> statefulSetPodDataPersistentVolumeClaimPattern.matcher(pvc.getMetadata().getName()).matches())
        .toList();
    var requiredOwnerReferences = List.of(
        ResourceUtil.getOwnerReference(context));

    Seq.seq(pvcsToProtect)
        .filter(pvc -> !Objects.equals(
            pvc.getMetadata().getOwnerReferences(),
            requiredOwnerReferences))
        .map(pvc -> fixPvcOwnerReferences(
            requiredOwnerReferences, pvc,
            deployedStatefulSet.getMetadata().getName()))
        .grouped(pvc -> pvc.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pvc -> protectHandler.patch(context, pvc, null));
  }

  private void fixPods(
      final T context,
      final StatefulSet statefulSet,
      final StatefulSet deployedStatefulSet,
      final Map<String, String> appLabel,
      PatroniCtlInstance patroniCtl) {
    var podsToFix = findStatefulSetPods(statefulSet, appLabel);
    List<Pod> disruptablePodsToPatch =
        fixNonDisruptablePods(context, statefulSet, patroniCtl, podsToFix);
    final List<Pod> podPatroniLabelsToPatch;
    if (!isPatroniOnKubernetes(context)) {
      podPatroniLabelsToPatch = fixPodsPatroniLabels(context, statefulSet, patroniCtl, podsToFix);
    } else {
      podPatroniLabelsToPatch = List.of();
    }
    List<Pod> podAnnotationsToPatch = fixPodsAnnotations(statefulSet, podsToFix);
    List<Pod> podOwnerReferencesToPatch = fixPodsOwnerReferences(
        context, deployedStatefulSet, podsToFix);
    List<Pod> podSelectorMatchLabelsToPatch =
        fixPodsSelectorMatchLabels(context, statefulSet, podsToFix);
    Seq.seq(disruptablePodsToPatch)
        .append(podPatroniLabelsToPatch)
        .append(podAnnotationsToPatch)
        .append(podOwnerReferencesToPatch)
        .append(podSelectorMatchLabelsToPatch)
        .grouped(pod -> pod.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pod -> handler.patch(context, pod, null));
  }

  private List<Pod> fixNonDisruptablePods(
      T context,
      StatefulSet statefulSet,
      PatroniCtlInstance patroniCtl,
      List<Pod> pods) {
    final Optional<String> latestPrimaryFromPatroni =
        PatroniUtil.getLatestPrimaryFromPatroni(patroniCtl);
    final var members = patroniCtl.list();
    final int replicas = statefulSet.getSpec().getReplicas();
    return pods.stream()
        .filter(pod -> isNonDisruptable(context, pod))
        .filter(pod -> members.stream()
            .filter(PatroniMember::isPrimary)
            .map(PatroniMember::getMember)
            .anyMatch(pod.getMetadata().getName()::equals))
        .filter(pod -> getPodIndex(pod) + 1 < replicas
            || latestPrimaryFromPatroni.map(pod.getMetadata().getName()::equals).orElse(false))
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
          + " since current or latest primary",
          namespace, podName, namespace, name);
    }
    pod.getMetadata().getLabels().put(labelFactory.labelMapper().disruptableKey(context),
        StackGresContext.RIGHT_VALUE);
    return pod;
  }

  private List<Pod> fixPodsPatroniLabels(
      T context,
      StatefulSet statefulSet,
      PatroniCtlInstance patroniCtl,
      List<Pod> pods) {
    var roles = patroniCtl.list()
        .stream()
        .map(member -> Tuple.tuple(member.getMember(), member.getLabelRole()))
        .filter(t -> t.v2 != null)
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));

    return Seq.seq(pods)
        .filter(pod -> roles.containsKey(pod.getMetadata().getName()))
        .map(pod -> Tuple.tuple(pod, roles.get(pod.getMetadata().getName())))
        .filter(t -> Optional.ofNullable(t.v1.getMetadata().getLabels())
            .stream()
            .map(Map::entrySet)
            .flatMap(Set::stream)
            .noneMatch(Map.entry(PatroniUtil.ROLE_KEY, t.v2)::equals))
        .map(t -> fixPodPatroniLabels(t.v1, t.v2))
        .append(pods.stream()
            .filter(pod -> !roles.containsKey(pod.getMetadata().getName()))
            .filter(pod -> Optional.ofNullable(pod.getMetadata().getLabels())
                .stream()
                .map(Map::keySet)
                .flatMap(Set::stream)
                .anyMatch(PatroniUtil.ROLE_KEY::equals))
            .map(pod -> removePodPatroniLabels(pod)))
        .toList();
  }

  private Pod fixPodPatroniLabels(Pod pod, String role) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing Patroni {} label for Pod {}.{} for StatefulSet {}.{} to {}",
          PatroniUtil.ROLE_KEY, namespace, podName, namespace, name, role);
    }
    pod.getMetadata().setLabels(Optional.ofNullable(pod.getMetadata().getLabels())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(label -> PatroniUtil.ROLE_KEY.equals(label.v1))
        .append(Tuple.tuple(PatroniUtil.ROLE_KEY, role))
        .toMap(Tuple2::v1, Tuple2::v2));
    return pod;
  }

  private Pod removePodPatroniLabels(Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Remove Patroni {} label for Pod {}.{} for StatefulSet {}.{}",
          PatroniUtil.ROLE_KEY, namespace, podName, namespace, name);
    }
    pod.getMetadata().setLabels(Optional.ofNullable(pod.getMetadata().getLabels())
        .map(Seq::seq)
        .orElse(Seq.of())
        .filter(label -> PatroniUtil.ROLE_KEY.equals(label.v1))
        .toMap(Tuple2::v1, Tuple2::v2));
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
      T context,
      StatefulSet statefulSet,
      List<Pod> pods) {
    var requiredOwnerReferences = List.of(
        new OwnerReferenceBuilder()
        .withApiVersion(statefulSet.getApiVersion())
        .withKind(statefulSet.getKind())
        .withName(statefulSet.getMetadata().getName())
        .withUid(statefulSet.getMetadata().getUid())
        .withBlockOwnerDeletion(true)
        .withController(true)
        .build(),
        ResourceUtil.getOwnerReference(context));

    return pods.stream()
        .filter(pod -> !Objects.equals(
            requiredOwnerReferences,
            pod.getMetadata().getOwnerReferences()))
        .map(pod -> fixPodOwnerReferences(
            requiredOwnerReferences, pod,
            statefulSet.getMetadata().getName()))
        .toList();
  }

  private Pod fixPodOwnerReferences(
      List<OwnerReference> requiredOwnerReferences,
      Pod pod,
      String stsName) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      LOGGER.debug("Fixing owner references for Pod {}.{} for StatefulSet {}.{} to {}",
          namespace, podName, namespace, stsName, requiredOwnerReferences);
    }
    pod.getMetadata().setOwnerReferences(requiredOwnerReferences);
    return pod;
  }

  private List<Pod> fixPodsSelectorMatchLabels(
      final T context,
      final StatefulSet statefulSet,
      final List<Pod> pods) {
    final var requiredPodLabels =
        Optional.ofNullable(statefulSet.getSpec().getSelector().getMatchLabels())
        .map(labels -> labels.entrySet().stream()
            .filter(label -> !labelFactory.labelMapper().disruptableKey(context)
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

  private void fixPvcs(
      T context,
      StatefulSet statefulSet,
      final StatefulSet deployedStatefulSet,
      Map<String, String> appLabel) {
    final String namespace = statefulSet.getMetadata().getNamespace();
    Pattern statefulSetPodDataPersistentVolumeClaimPattern = ResourceUtil.getNameWithIndexPattern(
        StackGresUtil.statefulSetPodDataPersistentVolumeClaimName(context));
    var pvcsToFix = pvcScanner.getResourcesInNamespaceWithLabels(namespace, appLabel).stream()
        .filter(pvc -> statefulSetPodDataPersistentVolumeClaimPattern.matcher(pvc.getMetadata().getName()).matches())
        .toList();
    List<PersistentVolumeClaim> pvcAnnotationsToPatch = fixPvcsAnnotations(
        statefulSet, pvcsToFix);
    List<PersistentVolumeClaim> pvcLabelsToPatch = fixPvcsLabels(
        statefulSet, pvcsToFix);
    List<PersistentVolumeClaim> pvcOwnerReferencesToPatch = fixPvcOwnerReferences(
        context, deployedStatefulSet, pvcsToFix);
    Seq.seq(pvcAnnotationsToPatch).append(pvcLabelsToPatch).append(pvcOwnerReferencesToPatch)
        .grouped(pvc -> pvc.getMetadata().getName()).map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get).forEach(pvc -> handler.patch(context, pvc, null));
  }

  private List<PersistentVolumeClaim> fixPvcsAnnotations(
      StatefulSet statefulSet,
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

  private PersistentVolumeClaim fixPvcAnnotations(
      Map<String, String> requiredPvcAnnotations,
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

  private List<PersistentVolumeClaim> fixPvcsLabels(
      StatefulSet statefulSet,
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

  private PersistentVolumeClaim fixPvcLabels(
      Map<String, String> requiredPvcLabels,
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

  private List<PersistentVolumeClaim> fixPvcOwnerReferences(
      T context,
      StatefulSet statefulSet,
      List<PersistentVolumeClaim> pvcs) {
    var requiredOwnerReferences = List.of(
        new OwnerReferenceBuilder()
        .withApiVersion(statefulSet.getApiVersion())
        .withKind(statefulSet.getKind())
        .withName(statefulSet.getMetadata().getName())
        .withUid(statefulSet.getMetadata().getUid())
        .withBlockOwnerDeletion(true)
        .withController(true)
        .build(),
        ResourceUtil.getOwnerReference(context));

    return pvcs.stream()
        .filter(pvc -> !Objects.equals(
            requiredOwnerReferences,
            pvc.getMetadata().getOwnerReferences()))
        .map(pvc -> fixPvcOwnerReferences(
            requiredOwnerReferences, pvc,
            statefulSet.getMetadata().getName()))
        .toList();
  }

  private PersistentVolumeClaim fixPvcOwnerReferences(
      List<OwnerReference> requiredOwnerReferences,
      PersistentVolumeClaim pvc,
      String stsName) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pvc.getMetadata().getNamespace();
      final String podName = pvc.getMetadata().getName();
      LOGGER.debug("Fixing owner references for PersistentVolumeClaim {}.{}"
          + " for StatefulSet {}.{} to {}",
          namespace, podName, namespace, stsName, requiredOwnerReferences);
    }
    pvc.getMetadata().setOwnerReferences(requiredOwnerReferences);
    return pvc;
  }

  private List<Pod> findStatefulSetPods(
      final StatefulSet updatedSts,
      final Map<String, String> appLabel) {
    final String namespace = updatedSts.getMetadata().getNamespace();
    final String name = updatedSts.getMetadata().getName();
    var stsPodNameMatcher = ResourceUtil.getNameWithIndexPattern(name);
    return podScanner.getResourcesInNamespaceWithLabels(namespace, appLabel).stream()
        .filter(pod -> Optional.of(pod)
            .map(Pod::getMetadata)
            .map(ObjectMeta::getName)
            .map(stsPodNameMatcher::matcher)
            .filter(Matcher::matches)
            .isPresent())
        .sorted(Comparator.comparing(this::getPodIndex))
        .toList();
  }

  private boolean isNonDisruptable(T context, Pod pod) {
    return !Objects.equals(
        pod.getMetadata().getLabels().get(labelFactory.labelMapper().disruptableKey(context)),
        StackGresContext.RIGHT_VALUE);
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
