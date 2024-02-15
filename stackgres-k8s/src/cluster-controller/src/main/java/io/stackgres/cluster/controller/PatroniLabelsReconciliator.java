/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.SafeReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniLabelsReconciliator extends SafeReconciliator<ClusterContext, Boolean> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniLabelsReconciliator.class);

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

  public static PatroniLabelsReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new PatroniLabelsReconciliator(parameters.findAny().get());
  }

  public ReconciliationResult<Boolean> safeReconcile(KubernetesClient client, ClusterContext context)
      throws Exception {
    final StackGresCluster cluster = context.getCluster();
    final var patroniCtl = this.patroniCtl.instanceFor(cluster);
    final Pod pod = podFinder
        .findByNameAndNamespace(podName, cluster.getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalStateException("Pod " + podName + " not found"));
    final AtomicBoolean roleUpdated = new AtomicBoolean(false);

    podWriter.update(pod, currentPod -> {
      final String role = patroniCtl.list()
          .stream()
          .filter(member -> podName.equals(member.getMember()))
          .findFirst()
          .map(PatroniMember::getLabelRole)
          .orElse(null);
      if (role == null) {
        if (Optional.ofNullable(currentPod.getMetadata().getLabels())
            .orElse(Map.of())
            .entrySet().stream().anyMatch(label -> label.getKey().equals(PatroniUtil.ROLE_KEY))) {
          currentPod.getMetadata().setLabels(currentPod.getMetadata().getLabels()
              .entrySet()
              .stream()
              .filter(label -> !label.getKey().equals(PatroniUtil.ROLE_KEY))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
          roleUpdated.set(true);
          LOGGER.info("Role was removed from Pod");
        }
      } else {
        if (Optional.ofNullable(currentPod.getMetadata().getLabels())
            .orElse(Map.of())
            .entrySet().stream().noneMatch(label -> label.equals(Map.entry(PatroniUtil.ROLE_KEY, role)))) {
          currentPod.getMetadata().setLabels(Stream
              .concat(
                  currentPod.getMetadata().getLabels()
                .entrySet()
                .stream()
                .filter(label -> !label.getKey().equals(PatroniUtil.ROLE_KEY)),
                Stream.of(Map.entry(PatroniUtil.ROLE_KEY, role)))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
          roleUpdated.set(true);
          LOGGER.info("Role {} was assigned to Pod", role);
        }
      }
    });

    return new ReconciliationResult<>(roleUpdated.get());
  }

}
