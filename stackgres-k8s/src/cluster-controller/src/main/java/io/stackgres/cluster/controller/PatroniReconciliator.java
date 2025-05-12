/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.common.ConfigFilesUtil.configChanged;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.ongres.process.FluentProcess;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.ClusterPatroniConfigEventReason;
import io.stackgres.cluster.common.PatroniCommandUtil;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationGroup;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresReplicationRole;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniReconciliator extends SafeReconciliator<StackGresClusterContext, Boolean> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniReconciliator.class);

  private static final Path PATRONI_START_FILE_PATH =
      Paths.get(ClusterPath.PATRONI_START_FILE_PATH.path());
  private static final Path PATRONI_CONFIG_PATH =
      Paths.get(ClusterPath.PATRONI_CONFIG_FILE_PATH.path());
  private static final Path LAST_PATRONI_CONFIG_PATH =
      Paths.get(ClusterPath.PATRONI_CONFIG_PATH.path()
          + "/last-" + ClusterPath.PATRONI_CONFIG_FILE_PATH.filename());

  private static final Pattern TAGS_LINE_PATTERN = Pattern.compile("^tags:.*$");
  private static final Pattern PG_CTL_TIMEOUT_LINE_PATTERN = Pattern.compile("^ pg_ctl_timeout:.*$");
  private static final Pattern CALLBACKS_LINE_PATTERN = Pattern.compile("^ callbacks:.*$");
  private static final Pattern PRE_PROMOTE_LINE_PATTERN = Pattern.compile("^ pre_promote:.*$");
  private static final Pattern BEFORE_STOP_LINE_PATTERN = Pattern.compile("^ before_stop:.*$");

  private static final String NOLOADBALANCE_TAG = PatroniUtil.NOLOADBALANCE_TAG;
  private static final String NOFAILOVER_TAG = PatroniUtil.NOFAILOVER_TAG;
  private static final String TRUE_TAG_VALUE = PatroniUtil.TRUE_TAG_VALUE;
  private static final String FALSE_TAG_VALUE = PatroniUtil.FALSE_TAG_VALUE;

  private static final AtomicBoolean STARTUP = new AtomicBoolean(false);

  private final Supplier<Boolean> reconcilePatroni;
  private final String podName;
  private final EventController eventController;
  private final ObjectMapper objectMapper;

  @Dependent
  public static class Parameters {
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject EventController eventController;
    @Inject ObjectMapper objectMapper;
  }

  @Inject
  public PatroniReconciliator(Parameters parameters) {
    this.reconcilePatroni = () -> parameters.propertyContext
        .getBoolean(ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PATRONI);
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.eventController = parameters.eventController;
    this.objectMapper = parameters.objectMapper;
  }

  public Boolean isStartup() {
    return STARTUP.get();
  }

  @Override
  public ReconciliationResult<Boolean> safeReconcile(KubernetesClient client,
      StackGresClusterContext context) {
    if (!reconcilePatroni.get()) {
      return new ReconciliationResult<>(false);
    }
    try {
      boolean statusUpdated = reconcilePatroni(client, context);
      return new ReconciliationResult<>(statusUpdated);
    } catch (IOException | RuntimeException ex) {
      LOGGER.error("An error occurred while reconciling patroni", ex);
      try {
        eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
            "An error occurred while reconciling patroni configuration: " + ex.getMessage(),
            client);
      } catch (Exception eventEx) {
        LOGGER.error("An error occurred while sending an event", eventEx);
      }
      return new ReconciliationResult<>(ex);
    }
  }

  /**
   * Implementation of replication groups is based on Patroni tags and Service
   *  labels. Depending on the role assigned Patroni tags are changed accordingly
   *  in the Patroni config file (`/etc/patroni/config.yml`) and the
   *  configuration is reloaded by sending signal `HUP` to the Patroni process.
   *  The same tags are also used to label the Pod and the label
   *  `noloadbalance: "false"` is added to the Service selector for replicas.
   * 
   * <p>
   * Groups including the implicit first group follow an absolute ordering.
   *  Implicit first group has index 0. Other groups have index equals to
   *  the index in the array where they are specified plus 1. This list of
   *  group is then flattened by the number of instances the group declare
   *  and mapped to the group role.
   * </p>
   * 
   * <p>
   * For instance, if we have a first implicit
   *  group of 3 instances with role `HA` (calculated by subtracting the sum
   *  of the instances in specified groups to the total number of instances),
   *  a second group of 2 instances with role `NONE`, a third group with 2
   *  instances with role `READONLY` and a fourth group with 1 instance with
   *  role `HA_READ` the list of replication roles is:
   * <ul>
   * <li>0: `HA`</li>
   * <li>1: `HA`</li>
   * <li>2: `HA`</li>
   * <li>3: `NONE`</li>
   * <li>4: `NONE`</li>
   * <li>5: `READONLY`</li>
   * <li>6: `READONLY`</li>
   * <li>7: `HA_READ`</li>
   * </ul>
   * Pods of the StatefulSet is then mapped 1 on 1 to this list using the index in their names.
   * </p>
   * 
   * <p>
   * If any file in /etc/ssl change will reload PostgreSQL config through patroni
   * </p>
   */
  private boolean reconcilePatroni(KubernetesClient client, StackGresClusterContext context)
      throws IOException {
    if (Files.exists(PATRONI_START_FILE_PATH)) {
      Files.setLastModifiedTime(PATRONI_START_FILE_PATH, FileTime.from(Instant.now()));
    } else {
      Files.createFile(PATRONI_START_FILE_PATH);
    }
    if (!STARTUP.get()) {
      STARTUP.set(true);
    }
    if (!Files.exists(PATRONI_CONFIG_PATH)) {
      LOGGER.warn("Can not reload patroni config since config file {} was not found, will retry later",
          PATRONI_CONFIG_PATH);
      return false;
    }
    final StackGresCluster cluster = context.getCluster();
    final Optional<Tuple2<StackGresReplicationRole, Long>> podReplicationRole =
        getPodAssignedReplicationRole(cluster, podName);
    if (podReplicationRole.isEmpty()) {
      LOGGER.warn("Can not reload patroni config since role for pod {} is unknown, will retry later",
          podName);
      return false;
    }
    final Map<String, String> tagsMap =
        getPatroniTagsForReplicationRole(podReplicationRole.get().v1);
    final String tags = getTagsAsYamlString(tagsMap);
    boolean tagsNeedsUpdate = Seq.seq(Files.readAllLines(PATRONI_CONFIG_PATH))
        .filter(line -> TAGS_LINE_PATTERN.matcher(line).matches())
        .noneMatch(tags::equals);
    if (tagsNeedsUpdate) {
      replacePatroniTags(tags);
    }
    final String pgCtlTimeout = getPgCtlTimeoutAsYamlString(cluster);
    boolean pgCtlTimeoutNeedsUpdate = Seq.seq(Files.readAllLines(PATRONI_CONFIG_PATH))
        .filter(line -> PG_CTL_TIMEOUT_LINE_PATTERN.matcher(line).matches())
        .noneMatch(pgCtlTimeout::equals);
    if (pgCtlTimeoutNeedsUpdate) {
      addOrReplacePatroniPostgresqlPgCtlTimeout(pgCtlTimeout);
    }
    final String callbacks = getCallbacksAsYamlString(cluster);
    boolean callbacksNeedsUpdate = Seq.seq(Files.readAllLines(PATRONI_CONFIG_PATH))
        .filter(line -> CALLBACKS_LINE_PATTERN.matcher(line).matches())
        .noneMatch(callbacks::equals);
    if (callbacksNeedsUpdate) {
      addOrReplacePatroniPostgresqlCallbacks(callbacks);
    }
    final String prePromote = getPrePromoteAsYamlString(cluster);
    boolean prePromoteNeedsUpdate = Seq.seq(Files.readAllLines(PATRONI_CONFIG_PATH))
        .filter(line -> PRE_PROMOTE_LINE_PATTERN.matcher(line).matches())
        .noneMatch(prePromote::equals);
    if (prePromoteNeedsUpdate) {
      addOrReplacePatroniPostgresqlPrePromote(prePromote);
    }
    final String beforeStop = getBeforeStopAsYamlString(cluster);
    boolean beforeStopNeedsUpdate = Seq.seq(Files.readAllLines(PATRONI_CONFIG_PATH))
        .filter(line -> BEFORE_STOP_LINE_PATTERN.matcher(line).matches())
        .noneMatch(beforeStop::equals);
    if (beforeStopNeedsUpdate) {
      addOrReplacePatroniPostgresqlBeforeStop(beforeStop);
    }
    if (configChanged(PATRONI_CONFIG_PATH, LAST_PATRONI_CONFIG_PATH)) {
      try {
        PatroniCommandUtil.reloadPatroniConfig();
      } catch (Exception ex) {
        LOGGER.warn("Can not reload patroni config now, will retry later: {}", ex.getMessage(), ex);
        return false;
      }
      setPatroniTagsAsPodLabels(client, cluster, podName, tagsMap);
      Files.copy(PATRONI_CONFIG_PATH, LAST_PATRONI_CONFIG_PATH,
          StandardCopyOption.REPLACE_EXISTING);
      LOGGER.info("Patroni config updated");
      eventController.sendEvent(ClusterPatroniConfigEventReason.CLUSTER_PATRONI_CONFIG_UPDATED,
          "Patroni config updated", client);
    }

    final boolean statusUpdated =
        setPodReplicatinGroupInClusterStatus(cluster, podReplicationRole.get().v2.intValue());
    return statusUpdated;
  }

  private boolean setPodReplicatinGroupInClusterStatus(final StackGresCluster cluster,
      final Integer podReplicationRoleIndex) {
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }
    if (cluster.getStatus().getPodStatuses() == null) {
      cluster.getStatus().setPodStatuses(new ArrayList<>());
    }
    if (cluster.getStatus().getPodStatuses().stream()
        .noneMatch(podStatus -> podStatus.getName().equals(podName))) {
      StackGresClusterPodStatus podStatus = new StackGresClusterPodStatus();
      podStatus.setName(podName);
      cluster.getStatus().getPodStatuses().add(podStatus);
    }
    final StackGresClusterPodStatus podStatus =
        cluster.getStatus().getPodStatuses().stream()
        .filter(status -> status.getName().equals(podName))
        .findAny().orElseThrow();
    boolean statusUpdated =
        !Objects.equals(podStatus.getReplicationGroup(), podReplicationRoleIndex);
    podStatus.setReplicationGroup(podReplicationRoleIndex);
    return statusUpdated;
  }

  private Optional<Tuple2<StackGresReplicationRole, Long>> getPodAssignedReplicationRole(
      final StackGresCluster cluster, final String podName) {
    final int podIndex = getPodIndex(podName);
    return Seq.seq(cluster.getSpec().getReplicationGroups())
        .zipWithIndex()
        .flatMap(group -> Seq.range(0, group.v1.getInstances()).map(i -> group))
        .zipWithIndex()
        .filter(t -> t.v2.intValue() == podIndex)
        .map(Tuple2::v1)
        .map(t -> t.map1(StackGresClusterReplicationGroup::getRole))
        .map(t -> t.map1(StackGresReplicationRole::fromString))
        .findFirst();
  }

  private int getPodIndex(String podName) {
    return Integer.parseInt(ResourceUtil.getIndexPattern().matcher(podName)
        .results().findFirst().orElseThrow().group(1));
  }

  private Map<String, String> getPatroniTagsForReplicationRole(
      final StackGresReplicationRole podReplicationRole) {
    switch (podReplicationRole) {
      case HA_READ:
        return Map.of(
            NOFAILOVER_TAG, FALSE_TAG_VALUE,
            NOLOADBALANCE_TAG, FALSE_TAG_VALUE);
      case HA:
        return Map.of(
            NOFAILOVER_TAG, FALSE_TAG_VALUE,
            NOLOADBALANCE_TAG, TRUE_TAG_VALUE);
      case READONLY:
        return Map.of(
            NOFAILOVER_TAG, TRUE_TAG_VALUE,
            NOLOADBALANCE_TAG, FALSE_TAG_VALUE);
      case NONE:
        return Map.of(
            NOFAILOVER_TAG, TRUE_TAG_VALUE,
            NOLOADBALANCE_TAG, TRUE_TAG_VALUE);
      default:
        throw new IllegalArgumentException(
            "Unknown replication role " + podReplicationRole);
    }
  }

  private String getTagsAsYamlString(final Map<String, String> tagsMap) {
    return String.format("tags: { %s }",
        Seq.seq(tagsMap).map(t -> t.v1 + ": " + t.v2).toString(", "));
  }

  private void replacePatroniTags(final String tagsAsYamlString) {
    FluentProcess.start("sed", "-i",
        String.format("s/^tags:.*$/%s/", tagsAsYamlString),
        PATRONI_CONFIG_PATH.toString()).join();
  }

  private String getPgCtlTimeoutAsYamlString(final StackGresCluster cluster) {
    return String.format("  pg_ctl_timeout: %s",
        Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .flatMap(StackGresClusterPatroniConfig::getPgCtlTimeout)
        .map(String::valueOf)
        .orElse("60"));
  }

  private void addOrReplacePatroniPostgresqlPgCtlTimeout(final String pgCtlTimeout) {
    var hasPgCtlTimeout =
        FluentProcess.start("grep", "-q", "^ *pg_ctl_timeout:.*$",
        PATRONI_CONFIG_PATH.toString()).tryGet();
    if (hasPgCtlTimeout.exception().isEmpty()) {
      FluentProcess.start("sed", "-i",
          String.format("s/^ *pg_ctl_timeout:.*$/%s/", pgCtlTimeout),
          PATRONI_CONFIG_PATH.toString()).join();
    } else {
      FluentProcess.start("sed", "-i",
          String.format("s/^postgresql:$/postgresql:\\n%s/", pgCtlTimeout),
          PATRONI_CONFIG_PATH.toString()).join();
    }
  }

  private String getCallbacksAsYamlString(final StackGresCluster cluster) {
    return String.format("  callbacks: %s",
        Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .flatMap(StackGresClusterPatroniConfig::getCallbacks)
        .<JsonNode>map(objectMapper::valueToTree)
        .map(JsonNode::toString)
        .orElse("{}"));
  }

  private void addOrReplacePatroniPostgresqlCallbacks(final String callbacks) {
    var hasCallbacks =
        FluentProcess.start("grep", "-q", "^ *callbacks:.*$",
        PATRONI_CONFIG_PATH.toString()).tryGet();
    String escapedCallbacks = callbacks
        .replace("\\", "\\\\")
        .replace("/", "\\/");
    if (hasCallbacks.exception().isEmpty()) {
      FluentProcess.start("sed", "-i",
          String.format("s/^ *callbacks:.*$/%s/", escapedCallbacks),
          PATRONI_CONFIG_PATH.toString()).join();
    } else {
      FluentProcess.start("sed", "-i",
          String.format("s/^postgresql:$/postgresql:\\n%s/", escapedCallbacks),
          PATRONI_CONFIG_PATH.toString()).join();
    }
  }

  private String getPrePromoteAsYamlString(final StackGresCluster cluster) {
    return String.format("  pre_promote: %s",
        Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .flatMap(StackGresClusterPatroniConfig::getPrePromote)
        .map(String::valueOf)
        .orElse(""));
  }

  private void addOrReplacePatroniPostgresqlPrePromote(final String prePromote) {
    var hasPrePromote =
        FluentProcess.start("grep", "-q", "^ *pre_promote:.*$",
        PATRONI_CONFIG_PATH.toString()).tryGet();
    String escapedPrePromote = prePromote
        .replace("\\", "\\\\")
        .replace("/", "\\/");
    if (hasPrePromote.exception().isEmpty()) {
      FluentProcess.start("sed", "-i",
          String.format("s/^ *pre_promote:.*$/%s/", escapedPrePromote),
          PATRONI_CONFIG_PATH.toString()).join();
    } else {
      FluentProcess.start("sed", "-i",
          String.format("s/^postgresql:$/postgresql:\\n%s/", escapedPrePromote),
          PATRONI_CONFIG_PATH.toString()).join();
    }
  }

  private String getBeforeStopAsYamlString(final StackGresCluster cluster) {
    return String.format("  before_stop: %s",
        Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .flatMap(StackGresClusterPatroniConfig::getBeforeStop)
        .map(String::valueOf)
        .orElse(""));
  }

  private void addOrReplacePatroniPostgresqlBeforeStop(final String beforeStop) {
    var hasBeforeStop =
        FluentProcess.start("grep", "-q", "^ *before_stop:.*$",
        PATRONI_CONFIG_PATH.toString()).tryGet();
    String escapedBeforeStop = beforeStop
        .replace("\\", "\\\\")
        .replace("/", "\\/");
    if (hasBeforeStop.exception().isEmpty()) {
      FluentProcess.start("sed", "-i",
          String.format("s/^ *before_stop:.*$/%s/", escapedBeforeStop),
          PATRONI_CONFIG_PATH.toString()).join();
    } else {
      FluentProcess.start("sed", "-i",
          String.format("s/^postgresql:$/postgresql:\\n%s/", escapedBeforeStop),
          PATRONI_CONFIG_PATH.toString()).join();
    }
  }

  private void setPatroniTagsAsPodLabels(
      final KubernetesClient client,
      final StackGresCluster cluster,
      final String podName,
      final Map<String, String> tagsMap) {
    KubernetesClientUtil.retryOnConflict(() -> {
      Pod pod = client.pods()
          .inNamespace(cluster.getMetadata().getNamespace())
          .withName(podName)
          .get();
      pod.getMetadata().setLabels(ImmutableMap.<String, String>builder()
          .putAll(Seq.seq(
              Optional.ofNullable(pod.getMetadata().getLabels())
              .orElse(Map.of())
              .entrySet())
              .filter(entry -> tagsMap.keySet().stream().noneMatch(entry.getKey()::equals)))
          .putAll(tagsMap)
          .build());
      client.pods()
          .resource(pod)
          .lockResourceVersion(pod.getMetadata().getResourceVersion())
          .update();
      return null;
    });
  }

}
