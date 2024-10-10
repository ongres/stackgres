/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static io.stackgres.common.StackGresKeys.ANNOTATIONS_TO_COMPONENT;

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
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniCtlInstance;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.ClusterRolloutUtil;
import io.stackgres.operator.common.ClusterRolloutUtil.PodRestartReason;
import io.stackgres.operator.common.ClusterRolloutUtil.PodRestartReasons;
import io.stackgres.operator.common.ClusterRolloutUtil.PostgresRestartReasons;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ReconciliationScope(value = StackGresCluster.class, kind = "StatefulSet")
@ApplicationScoped
public class ClusterStatefulSetWithPrimaryReconciliationHandler implements ReconciliationHandler<StackGresCluster> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(ClusterStatefulSetWithPrimaryReconciliationHandler.class);

  public static final Map<String, String> PLACEHOLDER_NODE_SELECTOR =
      Map.of("schedule", "this-pod-is-a-placeholder");

  private final StackGresContext context;

  private final ReconciliationHandler<StackGresCluster> handler;

  private final ReconciliationHandler<StackGresCluster> protectHandler;

  private final LabelFactoryForCluster labelFactory;

  private final ResourceFinder<StatefulSet> statefulSetFinder;

  private final ResourceScanner<Pod> podScanner;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final ResourceFinder<Secret> secretFinder;

  private final PatroniCtl patroniCtl;

  private final ObjectMapper objectMapper;

  @Inject
  public ClusterStatefulSetWithPrimaryReconciliationHandler(
      StackGresContext context,
      @ReconciliationScope(value = StackGresCluster.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresCluster> handler,
      LabelFactoryForCluster labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      ResourceFinder<Secret> secretFinder,
      PatroniCtl patroniCtl,
      ObjectMapper objectMapper) {
    this(context, handler, handler, labelFactory, statefulSetFinder, podScanner, pvcScanner, secretFinder,
        patroniCtl, objectMapper);
  }

  ClusterStatefulSetWithPrimaryReconciliationHandler(
      StackGresContext context,
      ReconciliationHandler<StackGresCluster> handler,
      ReconciliationHandler<StackGresCluster> protectHandler,
      LabelFactoryForCluster labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      ResourceFinder<Secret> secretFinder,
      PatroniCtl patroniCtl,
      ObjectMapper objectMapper) {
    this.context = context;
    this.handler = handler;
    this.protectHandler = protectHandler;
    this.labelFactory = labelFactory;
    this.statefulSetFinder = statefulSetFinder;
    this.podScanner = podScanner;
    this.pvcScanner = pvcScanner;
    this.secretFinder = secretFinder;
    this.patroniCtl = patroniCtl;
    this.objectMapper = objectMapper;
  }

  @Override
  public HasMetadata create(StackGresCluster cluster, HasMetadata resource) {
    return concileSts(cluster, resource, (c, sts) -> createStatefulSet(c, sts));
  }

  @Override
  public HasMetadata patch(StackGresCluster cluster, HasMetadata newResource,
      HasMetadata oldResource) {
    return concileSts(cluster, newResource, (c, sts) -> updateStatefulSet(c, sts));
  }

  @Override
  public HasMetadata replace(StackGresCluster cluster, HasMetadata resource) {
    return concileSts(cluster, resource, (c, sts) -> replaceStatefulSet(c, sts));
  }

  @Override
  public void delete(StackGresCluster cluster, HasMetadata resource) {
    handler.delete(cluster, safeCast(resource));
  }

  @Override
  public void deleteWithOrphans(StackGresCluster cluster, HasMetadata resource) {
    handler.deleteWithOrphans(cluster, safeCast(resource));
  }

  private StatefulSet safeCast(HasMetadata resource) {
    if (!(resource instanceof StatefulSet)) {
      throw new IllegalArgumentException("Resource must be a StatefulSet instance");
    }
    return (StatefulSet) resource;
  }

  private StatefulSet createStatefulSet(StackGresCluster cluster, StatefulSet requiredSts) {
    return (StatefulSet) handler.create(cluster, requiredSts);
  }

  private StatefulSet updateStatefulSet(StackGresCluster cluster, StatefulSet requiredSts) {
    try {
      return (StatefulSet) handler.patch(cluster, requiredSts, null);
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 422) {
        final Map<String, String> appLabel = labelFactory.appLabel();
        var deployedStatefulSet = statefulSetFinder.findByNameAndNamespace(
            requiredSts.getMetadata().getName(),
            requiredSts.getMetadata().getNamespace())
            .orElseThrow(() -> new RuntimeException(
                HasMetadata.getKind(cluster.getClass()) + " "
                + requiredSts.getMetadata().getNamespace()
                + "." + requiredSts.getMetadata().getName()
                + " not fount while replacing it"));

        protectPodsFromStatefulSetRemoval(cluster, deployedStatefulSet, appLabel);

        protectPvcsFromStatefulSetRemoval(cluster, deployedStatefulSet, appLabel);

        return replaceStatefulSet(cluster, requiredSts);
      } else {
        throw ex;
      }
    }
  }

  private StatefulSet replaceStatefulSet(StackGresCluster cluster, StatefulSet statefulSet) {
    handler.deleteWithOrphans(cluster, statefulSet);
    waitStatefulSetToBeDeleted(statefulSet);
    return (StatefulSet) handler.create(cluster, statefulSet);
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
      StackGresCluster cluster,
      HasMetadata resource,
      BiFunction<StackGresCluster, StatefulSet, StatefulSet> writer) {
    final StatefulSet requiredSts;
    try {
      requiredSts = objectMapper.treeToValue(
          objectMapper.valueToTree(safeCast(resource)), StatefulSet.class);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
    Optional<StatefulSet> currentSts = statefulSetFinder.findByNameAndNamespace(
        requiredSts.getMetadata().getName(),
        requiredSts.getMetadata().getNamespace());
    final Map<String, String> appLabel = labelFactory.appLabel();
    final List<Pod> currentPods = findStatefulSetPods(requiredSts, appLabel);
    final var patroniCtl = this.patroniCtl.instanceFor(cluster);
    final List<PatroniMember> patroniMembers = patroniCtl.list();
    final boolean isRolloutAllowed = ClusterRolloutUtil.isRolloutAllowed(cluster);
    final boolean isReducedImpact = ClusterRolloutUtil.isRolloutReducedImpact(cluster);
    final boolean requiresRestart = ClusterRolloutUtil
        .getPodsRestartReasons(cluster, currentSts, currentPods)
        .requiresRestart()
        || ClusterRolloutUtil
        .getPostgresRestartReasons(currentPods, patroniMembers)
        .requiresRestart();

    final int desiredReplicas;
    if (isRolloutAllowed && isReducedImpact && requiresRestart) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Increasing replicas to {} since rollout method is {}",
            requiredSts.getSpec().getReplicas() + 1,
            DbOpsMethodType.REDUCED_IMPACT.annotationValue());
      }
      desiredReplicas = requiredSts.getSpec().getReplicas() + 1;
    } else {
      desiredReplicas = requiredSts.getSpec().getReplicas();
    }
    final int lastReplicaIndex = desiredReplicas - 1;

    final Optional<String> latestPrimaryFromPatroni =
        PatroniUtil.getLatestPrimaryFromPatroni(patroniCtl);
    if (desiredReplicas > 0) {
      startPrimaryIfRemoved(cluster, requiredSts, appLabel, latestPrimaryFromPatroni, writer);
    }

    final List<Pod> pods = findStatefulSetPods(requiredSts, appLabel);
    if (desiredReplicas > 0) {
      pods.stream()
          .filter(pod -> latestPrimaryFromPatroni.map(pod.getMetadata().getName()::equals).orElse(false))
          .filter(pod -> getPodIndex(pod) > lastReplicaIndex)
          .filter(pod -> !isNonDisruptable(cluster, pod))
          .forEach(pod -> makePrimaryPodNonDisruptable(cluster, pod));
      long nonDisruptablePodsRemaining =
          countNonDisruptablePods(cluster, pods, lastReplicaIndex);
      int replicas = Math.max(0, (int) (desiredReplicas - nonDisruptablePodsRemaining));
      requiredSts.getSpec().setReplicas(replicas);
    } else {
      pods.stream()
          .filter(pod -> isNonDisruptable(cluster, pod))
          .forEach(pod -> makePrimaryPodDisruptable(cluster, pod));
      requiredSts.getSpec().setReplicas(0);
    }

    final var updatedSts = writer.apply(cluster, requiredSts);

    removeStatefulSetPlaceholderReplicas(cluster, requiredSts);

    fixPods(cluster, requiredSts, updatedSts, appLabel, patroniCtl);

    fixPvcs(cluster, requiredSts, updatedSts, appLabel);

    if (isRolloutAllowed) {
      performRollout(cluster, requiredSts, updatedSts, appLabel,
          latestPrimaryFromPatroni, patroniCtl, writer);
    }

    return updatedSts;
  }

  private void performRollout(
      StackGresCluster cluster,
      StatefulSet requiredSts,
      StatefulSet updatedSts,
      Map<String, String> appLabel,
      Optional<String> latestPrimaryFromPatroni,
      PatroniCtlInstance patroniCtl,
      BiFunction<StackGresCluster, StatefulSet, StatefulSet> writer) {
    List<Pod> pods = findStatefulSetPods(requiredSts, appLabel);
    final List<PatroniMember> patroniMembers = patroniCtl.list();
    PostgresRestartReasons postgresRestartReasons = ClusterRolloutUtil.getPostgresRestartReasons(
        pods,
        patroniMembers);
    PodRestartReasons podRestartReasons = ClusterRolloutUtil.getPodsRestartReasons(
        cluster,
        Optional.of(updatedSts),
        pods);
    if (!postgresRestartReasons.requiresRestart()
        && !podRestartReasons.requiresRestart()
        && pods.stream().noneMatch(ClusterRolloutUtil::isPodInFailedPhase)) {
      return;
    }
    final Optional<Pod> foundPrimaryPod = pods.stream()
        .filter(pod -> latestPrimaryFromPatroni.map(pod.getMetadata().getName()::equals).orElse(false))
        .findFirst();
    final Optional<Pod> foundPrimaryPodAndPendingRestart = foundPrimaryPod
        .filter(pod -> ClusterRolloutUtil
            .getPodRestartReasons(cluster, Optional.of(updatedSts), pod)
            .requiresRestart());
    final Optional<Pod> foundPrimaryPodAndPendingRestartAndFailed = foundPrimaryPodAndPendingRestart
        .filter(ClusterRolloutUtil::isPodInFailedPhase);
    if (foundPrimaryPodAndPendingRestartAndFailed.isPresent()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Re-creating primary Pod {} since pending restart and failed",
            foundPrimaryPodAndPendingRestartAndFailed.get().getMetadata().getName());
      }
      handler.delete(cluster, foundPrimaryPodAndPendingRestartAndFailed.get());
      return;
    }
    final Pod primaryPod = foundPrimaryPod.orElse(null);
    final List<Pod> otherPods = pods.stream()
        .filter(pod -> !Objects.equals(pod, primaryPod))
        .toList();
    final Optional<Pod> anyOtherPodAndPendingRestartAndFailed = otherPods
        .stream()
        .filter(pod -> ClusterRolloutUtil
            .getPodRestartReasons(cluster, Optional.of(updatedSts), pod)
            .requiresRestart())
        .filter(ClusterRolloutUtil::isPodInFailedPhase)
        .findAny();
    if (foundPrimaryPod.isEmpty()
        && anyOtherPodAndPendingRestartAndFailed.isPresent()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Re-creating non primary Pod {} since pending restart and failed (primary not found)",
            anyOtherPodAndPendingRestartAndFailed.get().getMetadata().getName());
      }
      handler.delete(cluster, anyOtherPodAndPendingRestartAndFailed.get());
      return;
    }
    final Optional<Pod> anyOtherPodAndPendingRestart = otherPods
        .stream()
        .filter(pod -> ClusterRolloutUtil
            .getPodRestartReasons(cluster, Optional.of(updatedSts), pod)
            .getReasons().contains(PodRestartReason.STATEFULSET))
        .findAny();
    if (foundPrimaryPod.isEmpty()
        && anyOtherPodAndPendingRestart.isPresent()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Re-creating non primary Pod {} since pending restart due to spec changes (primary not found)",
            anyOtherPodAndPendingRestart.get().getMetadata().getName());
      }
      handler.delete(cluster, anyOtherPodAndPendingRestart.get());
      return;
    }
    if (Seq.seq(foundPrimaryPod.stream())
        .append(otherPods)
        .anyMatch(Predicate.not(
            ((Predicate<Pod>) ClusterRolloutUtil::isPodInFailedPhase)
            .or(ClusterRolloutUtil::isPodReady)))) {
      LOGGER.debug("A Pod is not ready nor failing, wait for it to become ready or fail");
      return;
    }
    final Optional<Pod> anyOtherPodAndPendingRestartAnyReason = otherPods
        .stream()
        .filter(pod -> ClusterRolloutUtil
            .getPodRestartReasons(cluster, Optional.of(updatedSts), pod)
            .requiresRestart())
        .findAny();
    if (foundPrimaryPod.isEmpty()
        && anyOtherPodAndPendingRestartAnyReason.isPresent()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Re-creating non primary Pod {} since pending restart (primary not found)",
            anyOtherPodAndPendingRestartAnyReason.get().getMetadata().getName());
      }
      handler.delete(cluster, anyOtherPodAndPendingRestartAnyReason.get());
      return;
    }
    if (foundPrimaryPod
        .map(pod -> ClusterRolloutUtil.getPostgresRestartReasons(pod, patroniMembers)
            .requiresRestart())
        .orElse(false)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Restarting Postgres instance of primary Pod {} since pending restart",
            foundPrimaryPod.get().getMetadata().getName());
      }
      var credentials = getPatroniCredentials(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());
      patroniCtl.restart(credentials.v1, credentials.v2,
          foundPrimaryPod.get().getMetadata().getName());
      return;
    }
    var anyOtherPodAndPendingRestartInstance = otherPods
        .stream()
        .filter(pod -> ClusterRolloutUtil.getPostgresRestartReasons(pod, patroniMembers)
            .requiresRestart())
        .findFirst();
    if (anyOtherPodAndPendingRestartInstance.isPresent()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Restarting Postgres instance of non primary Pod {} since pending restart",
            anyOtherPodAndPendingRestartInstance.get().getMetadata().getName());
      }
      var credentials = getPatroniCredentials(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());
      patroniCtl.restart(credentials.v1, credentials.v2,
          anyOtherPodAndPendingRestartInstance.get().getMetadata().getName());
      return;
    }
    if (foundPrimaryPod.isPresent()
        && anyOtherPodAndPendingRestartAndFailed.isPresent()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Re-creating non primary Pod {} since pending restart and failed",
            anyOtherPodAndPendingRestartAndFailed.get().getMetadata().getName());
      }
      handler.delete(cluster, anyOtherPodAndPendingRestartAndFailed.get());
      return;
    }
    if (foundPrimaryPod.isPresent()
        && anyOtherPodAndPendingRestartAnyReason.isPresent()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Re-creating non primary Pod {} since pending restart",
            anyOtherPodAndPendingRestartAnyReason.get().getMetadata().getName());
      }
      handler.delete(cluster, anyOtherPodAndPendingRestartAnyReason.get());
      return;
    }
    if (foundPrimaryPodAndPendingRestart.isPresent()) {
      final Optional<PatroniMember> leastLagPatroniMemberAndReady =
          patroniMembers
          .stream()
          .filter(PatroniMember::isReplica)
          .filter(PatroniMember::isRunning)
          .filter(member -> Optional.ofNullable(member.getTags())
              .filter(tags -> tags.entrySet().stream().anyMatch(
                  tag -> tag.getKey().equals(PatroniUtil.NOFAILOVER_TAG)
                  && tag.getValue() != null && tag.getValue().getValue() != null
                  && Objects.equals(tag.getValue().getValue().toString(), Boolean.TRUE.toString())))
              .isEmpty())
          .min((m1, m2) -> {
            var l1 = Optional.ofNullable(m1.getLagInMb())
                .map(IntOrString::getIntVal);
            var l2 = Optional.ofNullable(m2.getLagInMb())
                .map(IntOrString::getIntVal);
            if (l1.isPresent() && l2.isPresent()) {
              return l1.get().compareTo(l2.get());
            } else if (l1.isPresent() && l2.isEmpty()) {
              return -1;
            } else if (l1.isEmpty() && l2.isPresent()) {
              return 1;
            } else {
              return 0;
            }
          });
      final Optional<Pod> otherLeastLagPodAndReady = leastLagPatroniMemberAndReady
          .stream()
          .flatMap(member -> otherPods
              .stream()
              .filter(ClusterRolloutUtil::isPodReady)
              .filter(pod -> member.getMember().equals(pod.getMetadata().getName())))
          .findFirst();
      if (otherLeastLagPodAndReady.isPresent()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Failover primary Pod {} to Pod {} since ready and with least lag",
              foundPrimaryPod.get().getMetadata().getName(),
              otherLeastLagPodAndReady.get().getMetadata().getName());
        }
        var credentials = getPatroniCredentials(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());
        patroniCtl.switchover(
            credentials.v1,
            credentials.v2,
            foundPrimaryPod.get().getMetadata().getName(),
            otherLeastLagPodAndReady.get().getMetadata().getName());
        return;
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Re-creating primary Pod {} since pending restart",
              foundPrimaryPodAndPendingRestart.get().getMetadata().getName());
        }
        handler.delete(cluster, foundPrimaryPodAndPendingRestart.get());
        return;
      }
    }
  }

  public Tuple2<String, String> getPatroniCredentials(String clusterName, String namespace) {
    return Optional
        .ofNullable(secretFinder
            .findByNameAndNamespace(
                PatroniUtil.secretName(clusterName),
                namespace))
            .orElseThrow(() -> new RuntimeException("Can not find Secret " + PatroniUtil.secretName(clusterName)))
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .map(date -> Tuple.tuple(
            Optional.ofNullable(date.get(StackGresPasswordKeys.RESTAPI_USERNAME_KEY))
            .orElseThrow(() -> new RuntimeException("Can not find key "
                + StackGresPasswordKeys.RESTAPI_USERNAME_KEY
                + " in Secret " + PatroniUtil.secretName(clusterName))),
            Optional.ofNullable(date.get(StackGresPasswordKeys.RESTAPI_PASSWORD_KEY))
            .orElseThrow(() -> new RuntimeException("Can not find key "
                + StackGresPasswordKeys.RESTAPI_PASSWORD_KEY
                + " in Secret " + PatroniUtil.secretName(clusterName)))))
        .orElseThrow(() -> new RuntimeException(
            "Can not find Secret " + PatroniUtil.secretName(clusterName)));
  }

  private void startPrimaryIfRemoved(StackGresCluster cluster, StatefulSet requiredSts,
      Map<String, String> appLabel, Optional<String> latestPrimaryFromPatroni,
      BiFunction<StackGresCluster, StatefulSet, StatefulSet> writer) {
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
      writer.apply(cluster, requiredSts);
      waitStatefulSetReplicasToBeCreated(requiredSts);
      LOGGER.debug("Creating primary Pod that was {} for StatefulSet {}.{}",
          latestPrimaryFromPatroni, namespace, name);
      requiredSts.getSpec().getTemplate().getSpec().setNodeSelector(nodeSelector);
      requiredSts.getSpec().setReplicas(
          latestPrimaryFromPatroni.map(ResourceUtil::getIndexFromNameWithIndex).orElse(0) + 1);
      writer.apply(cluster, requiredSts);
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

  private void removeStatefulSetPlaceholderReplicas(StackGresCluster cluster, StatefulSet statefulSet) {
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
          handler.delete(cluster, pod);
        });
  }

  private void makePrimaryPodNonDisruptable(StackGresCluster cluster, Pod primaryPod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = primaryPod.getMetadata().getNamespace();
      final String podName = primaryPod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Marking primary Pod {}.{} for StatefulSet {}.{} as non disruptible"
          + " since in the last index", namespace, podName, namespace, name);
    }
    final Map<String, String> primaryPodLabels = primaryPod.getMetadata().getLabels();
    primaryPodLabels.put(labelFactory.labelMapper().disruptableKey(cluster),
        StackGresKeys.WRONG_VALUE);
    handler.patch(cluster, primaryPod, null);
  }

  private void makePrimaryPodDisruptable(StackGresCluster cluster, Pod primaryPod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = primaryPod.getMetadata().getNamespace();
      final String podName = primaryPod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Marking primary Pod {}.{} for StatefulSet {}.{} as disruptible"
          + " since 0 desired replicas", namespace, podName, namespace, name);
    }
    final Map<String, String> primaryPodLabels = primaryPod.getMetadata().getLabels();
    primaryPodLabels.put(labelFactory.labelMapper().disruptableKey(cluster),
        StackGresKeys.RIGHT_VALUE);
    handler.patch(cluster, primaryPod, null);
  }

  private long countNonDisruptablePods(
      StackGresCluster cluster,
      List<Pod> pods,
      int lastReplicaIndex) {
    return pods.stream()
        .filter(pod -> isNonDisruptable(cluster, pod))
        .map(this::getPodIndex)
        .filter(pod -> pod > lastReplicaIndex)
        .count();
  }

  private void protectPodsFromStatefulSetRemoval(
      final StackGresCluster cluster,
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
        ResourceUtil.getOwnerReference(cluster));

    Seq.seq(podsToProtect)
        .filter(pod -> !Objects.equals(
            pod.getMetadata().getOwnerReferences(),
            requiredOwnerReferences))
        .map(pod -> fixPodOwnerReferences(
            requiredOwnerReferences, pod,
            deployedStatefulSet.getMetadata().getName()))
        .grouped(pod -> pod.getMetadata().getName())
        .map(Tuple2::v2).map(Seq::findFirst)
        .map(Optional::get)
        .forEach(pod -> protectHandler.patch(cluster, pod, null));
  }

  private void protectPvcsFromStatefulSetRemoval(
      StackGresCluster cluster,
      StatefulSet deployedStatefulSet,
      Map<String, String> appLabel) {
    final String namespace = deployedStatefulSet.getMetadata().getNamespace();
    Pattern statefulSetPodDataPersistentVolumeClaimPattern = ResourceUtil.getNameWithIndexPattern(
        StackGresUtil.statefulSetPodDataPersistentVolumeClaimName(cluster));
    var pvcsToProtect = pvcScanner.getResourcesInNamespaceWithLabels(namespace, appLabel).stream()
        .filter(pvc -> statefulSetPodDataPersistentVolumeClaimPattern.matcher(pvc.getMetadata().getName()).matches())
        .toList();
    var requiredOwnerReferences = List.of(
        ResourceUtil.getOwnerReference(cluster));

    Seq.seq(pvcsToProtect)
        .filter(pvc -> !Objects.equals(
            pvc.getMetadata().getOwnerReferences(),
            requiredOwnerReferences))
        .map(pvc -> fixPvcOwnerReferences(
            requiredOwnerReferences, pvc,
            deployedStatefulSet.getMetadata().getName()))
        .grouped(pvc -> pvc.getMetadata().getName())
        .map(Tuple2::v2)
        .map(Seq::findFirst)
        .map(Optional::get)
        .forEach(pvc -> protectHandler.patch(cluster, pvc, null));
  }

  private void fixPods(
      final StackGresCluster cluster,
      final StatefulSet statefulSet,
      final StatefulSet deployedStatefulSet,
      final Map<String, String> appLabel,
      PatroniCtlInstance patroniCtl) {
    var podsToFix = findStatefulSetPods(statefulSet, appLabel);
    List<Pod> disruptablePodsToPatch =
        fixNonDisruptablePods(cluster, statefulSet, patroniCtl, podsToFix);
    final List<Pod> podPatroniLabelsToPatch;
    if (!isPatroniOnKubernetes(cluster)) {
      podPatroniLabelsToPatch = fixPodsPatroniLabels(cluster, statefulSet, patroniCtl, podsToFix);
    } else {
      podPatroniLabelsToPatch = List.of();
    }
    List<Pod> podAnnotationsToPatch = fixPodsAnnotations(statefulSet, podsToFix);
    List<Pod> podOwnerReferencesToPatch = fixPodsOwnerReferences(
        cluster, deployedStatefulSet, podsToFix);
    List<Pod> podLabelsToPatch =
        fixPodsLabels(cluster, statefulSet, podsToFix);
    Seq.seq(disruptablePodsToPatch)
        .append(podPatroniLabelsToPatch)
        .append(podAnnotationsToPatch)
        .append(podOwnerReferencesToPatch)
        .append(podLabelsToPatch)
        .grouped(pod -> pod.getMetadata().getName())
        .map(Tuple2::v2)
        .map(Seq::findFirst)
        .map(Optional::get)
        .forEach(pod -> handler.patch(cluster, pod, null));
  }

  private List<Pod> fixNonDisruptablePods(
      StackGresCluster cluster,
      StatefulSet statefulSet,
      PatroniCtlInstance patroniCtl,
      List<Pod> pods) {
    final Optional<String> latestPrimaryFromPatroni =
        PatroniUtil.getLatestPrimaryFromPatroni(patroniCtl);
    final var members = patroniCtl.list();
    final int replicas = statefulSet.getSpec().getReplicas();
    return pods.stream()
        .filter(pod -> isNonDisruptable(cluster, pod))
        .filter(pod -> members.stream()
            .filter(PatroniMember::isPrimary)
            .map(PatroniMember::getMember)
            .anyMatch(pod.getMetadata().getName()::equals))
        .filter(pod -> getPodIndex(pod) + 1 < replicas
            || latestPrimaryFromPatroni.map(pod.getMetadata().getName()::equals).orElse(false))
        .filter(pod -> getPodIndex(pod) < replicas)
        .map(pod -> fixNonDisruptablePod(cluster, pod))
        .toList();
  }

  private Pod fixNonDisruptablePod(StackGresCluster cluster, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing non disruptable Pod {}.{} for StatefulSet {}.{} as disruptible"
          + " since current or latest primary",
          namespace, podName, namespace, name);
    }
    pod.getMetadata().getLabels().put(labelFactory.labelMapper().disruptableKey(cluster),
        StackGresKeys.RIGHT_VALUE);
    return pod;
  }

  private List<Pod> fixPodsPatroniLabels(
      StackGresCluster cluster,
      StatefulSet statefulSet,
      PatroniCtlInstance patroniCtl,
      List<Pod> pods) {
    final String patroniVersion = StackGresUtil.getPatroniVersion(context, cluster);
    final int patroniMajorVersion = StackGresUtil.getPatroniMajorVersion(patroniVersion);
    var roles = patroniCtl.list()
        .stream()
        .map(member -> Tuple.tuple(member.getMember(), member.getLabelRole(patroniMajorVersion)))
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
        .filter(label -> !PatroniUtil.ROLE_KEY.equals(label.v1))
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
        .filter(label -> !PatroniUtil.ROLE_KEY.equals(label.v1))
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
      StackGresCluster cluster,
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
        ResourceUtil.getOwnerReference(cluster));

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

  private List<Pod> fixPodsLabels(
      final StackGresCluster cluster,
      final StatefulSet statefulSet,
      final List<Pod> pods) {
    final var requiredPodLabels =
        Optional.ofNullable(statefulSet.getSpec().getTemplate().getMetadata().getLabels())
        .map(labels -> labels.entrySet().stream()
            .filter(label -> !labelFactory.labelMapper().disruptableKey(cluster)
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
        .map(pod -> fixPodLabels(requiredPodLabels, pod))
        .toList();
  }

  private Pod fixPodLabels(Map<String, String> requiredPodLabels, Pod pod) {
    if (LOGGER.isDebugEnabled()) {
      final String namespace = pod.getMetadata().getNamespace();
      final String podName = pod.getMetadata().getName();
      final String name = podName.substring(0, podName.lastIndexOf("-"));
      LOGGER.debug("Fixing labels for Pod {}.{} for StatefulSet {}.{} to {}",
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
      StackGresCluster cluster,
      StatefulSet statefulSet,
      final StatefulSet deployedStatefulSet,
      Map<String, String> appLabel) {
    final String namespace = statefulSet.getMetadata().getNamespace();
    Pattern statefulSetPodDataPersistentVolumeClaimPattern = ResourceUtil.getNameWithIndexPattern(
        StackGresUtil.statefulSetPodDataPersistentVolumeClaimName(cluster));
    var pvcsToFix = pvcScanner.getResourcesInNamespaceWithLabels(namespace, appLabel).stream()
        .filter(pvc -> statefulSetPodDataPersistentVolumeClaimPattern.matcher(pvc.getMetadata().getName()).matches())
        .toList();
    List<PersistentVolumeClaim> pvcAnnotationsToPatch = fixPvcsAnnotations(
        statefulSet, pvcsToFix);
    List<PersistentVolumeClaim> pvcLabelsToPatch = fixPvcsLabels(
        statefulSet, pvcsToFix);
    List<PersistentVolumeClaim> pvcOwnerReferencesToPatch = fixPvcOwnerReferences(
        cluster, deployedStatefulSet, pvcsToFix);
    Seq.seq(pvcAnnotationsToPatch)
        .append(pvcLabelsToPatch)
        .append(pvcOwnerReferencesToPatch)
        .grouped(pvc -> pvc.getMetadata().getName())
        .map(Tuple2::v2)
        .map(Seq::findFirst)
        .map(Optional::get)
        .forEach(pvc -> handler.patch(cluster, pvc, null));
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
      StackGresCluster cluster,
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
        ResourceUtil.getOwnerReference(cluster));

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

  private boolean isNonDisruptable(StackGresCluster cluster, Pod pod) {
    return !Objects.equals(
        pod.getMetadata().getLabels().get(labelFactory.labelMapper().disruptableKey(cluster)),
        StackGresKeys.RIGHT_VALUE);
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

  private boolean isPatroniOnKubernetes(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec().getConfigurations())
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(StackGresClusterPatroniConfig::isPatroniOnKubernetes)
        .orElse(true);
  }

}
