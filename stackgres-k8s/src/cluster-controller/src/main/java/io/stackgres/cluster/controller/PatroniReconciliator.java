/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.common.PatroniUtil.FALSE_TAG_VALUE;
import static io.stackgres.common.PatroniUtil.NOFAILOVER_TAG;
import static io.stackgres.common.PatroniUtil.NOLOADBALANCE_TAG;
import static io.stackgres.common.PatroniUtil.TRUE_TAG_VALUE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import com.ongres.process.FluentProcess;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.ClusterPatroniConfigEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationGroup;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresReplicationRole;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "This is not a bug if working with containers")
public class PatroniReconciliator {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniReconciliator.class);

  private static final Path PATRONI_CONF_PATH = Paths.get("/etc/patroni/postgres.yml");
  private static final Path LAST_PATRONI_CONF_PATH = Paths.get("/etc/patroni/last-postgres.yml");

  private static final Pattern TAGS_LINE_PATTERN = Pattern.compile("^tags:.*$");
  private static final Pattern PATRONI_COMMAND_PATTERN =
      Pattern.compile("^[^ ]+ /usr/bin/patroni .*$");

  private final boolean reconcilePatroni;
  private final String podName;
  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject EventController eventController;
  }

  @Inject
  public PatroniReconciliator(Parameters parameters) {
    this.reconcilePatroni = parameters.propertyContext
        .getBoolean(ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PATRONI);
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.eventController = parameters.eventController;
  }

  public ReconciliationResult<Boolean> reconcile(KubernetesClient client,
      StackGresClusterContext context) {
    if (!reconcilePatroni) {
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
   * <p>
   * Implementation of replication groups is based on Patroni tags and Service
   *  labels. Depending on the role assigned Patroni tags are changed accordingly
   *  in the Patroni config file (`/etc/patroni/postgres.yml`) and the
   *  configuration is reloaded by sending signal `HUP` to the Patroni process.
   *  The same tags are also used to label the Pod and the label
   *  `noloadbalance: "false"` is added to the Service selector for replicas.
   * </p>
   * <p>
   * Groups including the implicit first group follow an absolute ordering.
   *  Implicit first group has index 0. Other groups have index equals to
   *  the index in the array where they are specified plus 1. This list of
   *  group is then flattened by the number of instances the group declare
   *  and mapped to the group role.
   * </p>
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
   */
  private boolean reconcilePatroni(KubernetesClient client, StackGresClusterContext context)
      throws IOException {
    final StackGresCluster cluster = context.getCluster();
    final Optional<Tuple2<StackGresReplicationRole, Long>> podReplicationRole =
        getPodAssignedReplicationRole(cluster);
    if (podReplicationRole.isEmpty()) {
      return false;
    }
    final Map<String, String> tagsMap =
        getPatroniTagsForReplicationRole(podReplicationRole.get());
    final String tags = getTagsAsYamlString(tagsMap);
    boolean needsUpdate = Seq.seq(Files.readAllLines(PATRONI_CONF_PATH))
        .filter(line -> TAGS_LINE_PATTERN.matcher(line).matches())
        .noneMatch(tags::equals);
    if (needsUpdate) {
      replacePatroniTags(tags);
    }
    if (configChanged()) {
      reloadPatroniConfig();
      setPatroniTagsAsPodLabels(client, cluster, tagsMap);
      Files.copy(PATRONI_CONF_PATH, LAST_PATRONI_CONF_PATH,
          StandardCopyOption.REPLACE_EXISTING);
      LOGGER.info("Patroni config updated");
      eventController.sendEvent(ClusterPatroniConfigEventReason.CLUSTER_PATRONI_CONFIG_UPDATED,
          "Patroni config updated", client);
    }
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
      final StackGresCluster cluster) {
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

  @SuppressFBWarnings(value = "DB_DUPLICATE_SWITCH_CLAUSES",
      justification = "False positive")
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
        PATRONI_CONF_PATH.toString()).join();
  }

  private boolean configChanged() throws IOException {
    final List<String> patroniConfigLines = Files.readAllLines(PATRONI_CONF_PATH);
    return !Files.exists(LAST_PATRONI_CONF_PATH)
        || !Seq.seq(Files.readAllLines(LAST_PATRONI_CONF_PATH))
        .zipWithIndex()
        .allMatch(line -> Seq.seq(patroniConfigLines)
            .zipWithIndex()
            .anyMatch(line::equals));
  }

  private void reloadPatroniConfig() {
    final String patroniPid = findPatroniPid();
    FluentProcess.start("sh", "-c",
        String.format("kill -s HUP %s", patroniPid)).join();
  }

  private String findPatroniPid() {
    return ProcessHandle.allProcesses()
        .filter(process -> process.info().commandLine()
            .map(command -> PATRONI_COMMAND_PATTERN.matcher(command).matches())
            .orElse(false))
        .map(ProcessHandle::pid)
        .map(String::valueOf)
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Patroni process not found"));
  }

  private void setPatroniTagsAsPodLabels(KubernetesClient client, final StackGresCluster cluster,
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
          .inNamespace(cluster.getMetadata().getNamespace())
          .withName(podName)
          .lockResourceVersion(pod.getMetadata().getResourceVersion())
          .replace(pod);
      return null;
    });
  }

}
