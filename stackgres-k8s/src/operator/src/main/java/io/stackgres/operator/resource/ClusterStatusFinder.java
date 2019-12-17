/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileList;
import io.stackgres.operator.patroni.StackGresStatefulSet;
import io.stackgres.operator.resource.dto.ClusterPodStatus;
import io.stackgres.operator.resource.dto.ClusterStatus;
import io.stackgres.operator.rest.PatroniStatsScripts;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;

@ApplicationScoped
public class ClusterStatusFinder implements KubernetesCustomResourceFinder<ClusterStatus> {

  @Inject
  KubernetesCustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  KubernetesClientFactory kubClientFactory;

  public ClusterStatusFinder(KubernetesClientFactory kubClientFactory,
                             KubernetesCustomResourceFinder<StackGresCluster> clusterFinder) {
    this.kubClientFactory = kubClientFactory;
    this.clusterFinder = clusterFinder;
  }

  public ClusterStatusFinder() {
  }

  @Override
  public Optional<ClusterStatus> findByNameAndNamespace(String name, String namespace) {

    return clusterFinder.findByNameAndNamespace(name, namespace).map(cluster -> {

      ClusterStatus status = new ClusterStatus();

      try (KubernetesClient client = kubClientFactory.create()) {

        Optional<Pod> masterPod = client.pods()
            .inNamespace(cluster.getMetadata().getNamespace())
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(ResourceUtil.defaultLabels(cluster.getMetadata().getName()))
                .put("role", "master")
                .build())
            .list()
            .getItems()
            .stream()
            .findAny();

        ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME)
            .flatMap(crd -> Optional.ofNullable(client.customResources(crd,
                StackGresProfile.class,
                StackGresProfileList.class,
                StackGresProfileDoneable.class)
                .inNamespace(cluster.getMetadata().getNamespace())
                .withName(cluster.getSpec().getResourceProfile())
                .get()))
            .ifPresent(profile -> {
              status.setCpuRequested(profile.getSpec().getCpu());
              status.setMemoryRequested(profile.getSpec().getMemory());
            });

        status.setCpuFound(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getCpuFound())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setMemoryFound(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getMemoryFound())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setMemoryUsed(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getMemoryUsed())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setDiskFound(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getDiskFound())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setDiskUsed(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getDiskUsed())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setAverageLoad1m(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getLoad1m())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setAverageLoad5m(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getLoad5m())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setAverageLoad10m(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getLoad10m())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setPods(client.pods()
            .inNamespace(cluster.getMetadata().getNamespace())
            .withLabels(ResourceUtil.defaultLabels(cluster.getMetadata().getName()))
            .list()
            .getItems()
            .stream()
            .map(pod -> Tuple.tuple(pod, new ClusterPodStatus()))
            .peek(t -> t.v2.setNamespace(t.v1.getMetadata().getNamespace()))
            .peek(t -> t.v2.setName(t.v1.getMetadata().getName()))
            .peek(t -> t.v2.setRole(t.v1.getMetadata().getLabels().get("role")))
            .peek(t -> t.v2.setIp(t.v1.getStatus().getPodIP()))
            .peek(t -> t.v2.setStatus(t.v1.getStatus().getPhase()))
            .peek(t -> t.v2.setContainers(String.valueOf(t.v1.getSpec()
                .getContainers().size())))
            .peek(t -> t.v2.setContainersReady(String.valueOf(t.v1.getStatus()
                .getContainerStatuses()
                .stream()
                .filter(ContainerStatus::getReady)
                .count())))
            .map(t -> t.v2)
            .collect(Collectors.toList()));

        status.setPodsReady(String.valueOf(status
            .getPods()
            .stream()
            .filter(pod -> pod.getContainers().equals(pod.getContainersReady()))
            .count()));

        return status;
      }
    });

  }

  private List<String> exec(KubernetesClient client, Pod pod, String... args)
      throws Exception {
    return PodExec.exec(client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME, args);
  }

}
