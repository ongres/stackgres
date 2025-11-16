/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.AnyType;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniLabelsReconciliator extends SafeReconciliator<ClusterContext, Boolean> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniLabelsReconciliator.class);

  private static final List<String> PATRONI_LABELS = List.of(
      PatroniUtil.ROLE_KEY,
      PatroniUtil.CLONEFROM_TAG,
      PatroniUtil.FAILOVER_PRIORITY_TAG,
      PatroniUtil.NOFAILOVER_TAG,
      PatroniUtil.NOLOADBALANCE_TAG,
      PatroniUtil.NOSTREAM_TAG,
      PatroniUtil.NOSYNC_TAG,
      PatroniUtil.REPLICATEFROM_TAG);

  private static final List<String> PATRONI_FLAG_LABELS = List.of(
      PatroniUtil.NOLOADBALANCE_TAG,
      PatroniUtil.NOFAILOVER_TAG,
      PatroniUtil.NOSTREAM_TAG,
      PatroniUtil.NOSYNC_TAG);

  private final String podName;
  private final PatroniCtl patroniCtl;
  private final ResourceFinder<Pod> podFinder;
  private final ResourceWriter<Pod> podWriter;

  @Dependent
  public static class Parameters {
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject PatroniCtl patroniCtl;
    @Inject ResourceFinder<Pod> podFinder;
    @Inject ResourceWriter<Pod> podWriter;
  }

  @Inject
  public PatroniLabelsReconciliator(Parameters parameters) {
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.patroniCtl = parameters.patroniCtl;
    this.podFinder = parameters.podFinder;
    this.podWriter = parameters.podWriter;
  }

  public ReconciliationResult<Boolean> safeReconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    final StackGresCluster cluster = context.getCluster();
    final var patroniCtl = this.patroniCtl.instanceFor(cluster);
    final Pod pod = podFinder
        .findByNameAndNamespace(podName, cluster.getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalStateException("Pod " + podName + " not found"));
    final AtomicBoolean patroniLabelsUpdated = new AtomicBoolean(false);
    final String patroniVersion = StackGresUtil.getPatroniVersion(cluster);
    final int patroniMajorVersion = StackGresUtil.getPatroniMajorVersion(patroniVersion);

    podWriter.update(pod, currentPod -> {
      final var patroniMember = patroniCtl.list()
          .stream()
          .filter(member -> podName.equals(member.getMember()))
          .findFirst();
      final Optional<Map.Entry<String, String>> roleLabel = patroniMember
          .map(member -> member.getLabelRole(patroniMajorVersion))
          .map(role -> Map.entry(PatroniUtil.ROLE_KEY, role));
      final Map<String, String> patroniLabels =
          Seq.seq(roleLabel.stream())
          .append(patroniMember
              .map(member -> member.getTags())
              .map(Map::entrySet)
              .stream()
              .flatMap(Set::stream)
              .filter(tag -> PATRONI_LABELS.contains(tag.getKey()))
              .collect(Collectors.toMap(
                  Map.Entry::getKey,
                  Function.<Map.Entry<String, AnyType>>identity()
                  .andThen(Map.Entry::getValue)
                  .andThen(AnyType::toString)))
              .entrySet())
          .toMap(Map.Entry::getKey, Map.Entry::getValue);
      Map<String, String> currentLabels = currentPod.getMetadata().getLabels();
      currentPod.getMetadata().setLabels(
          Seq.seq(Optional.ofNullable(currentPod.getMetadata().getLabels())
              .map(Map::entrySet)
              .stream()
              .flatMap(Set::stream))
          .filter(label -> !PATRONI_LABELS.contains(label.getKey()))
          .append(patroniLabels.entrySet().stream()
              .filter(entry -> !PATRONI_FLAG_LABELS.contains(entry.getKey())))
          .append(PATRONI_FLAG_LABELS
              .stream()
              .flatMap(tag -> Optional.ofNullable(patroniLabels.get(tag))
                  .flatMap(label -> Optional.<Map.Entry<String, String>>empty())
                  .or(() -> Optional.of(Map.entry(tag, PatroniUtil.FALSE_TAG_VALUE)))
                  .stream()))
          .toMap(Map.Entry::getKey, Map.Entry::getValue));
      if (!Objects.equals(currentLabels, currentPod.getMetadata().getLabels())) {
        patroniLabelsUpdated.set(true);
        String currentRole = currentLabels.get(PatroniUtil.ROLE_KEY);
        if (roleLabel.isEmpty()) {
          if (currentRole != null) {
            LOGGER.debug("Role was removed from Pod");
          }
        } else {
          if (!Objects.equals(
              currentRole,
              roleLabel.get().getValue())) {
            LOGGER.debug("Role {} was assigned to Pod", roleLabel.get().getValue());
          }
        }
      }
    });

    return new ReconciliationResult<>(patroniLabelsUpdated.get());
  }

}
