/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.crd.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.StackGresUserClusterContext;
import io.stackgres.operator.patroni.factory.Patroni;
import io.stackgres.operator.rest.PatroniStatsScripts;
import io.stackgres.operator.rest.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class ClusterResourceConsumptionFinder
    implements CustomResourceFinder<ClusterResourceConsumtionDto> {

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  KubernetesClientFactory kubClientFactory;

  public ClusterResourceConsumptionFinder(
      KubernetesClientFactory kubClientFactory,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.kubClientFactory = kubClientFactory;
    this.clusterFinder = clusterFinder;
  }

  public ClusterResourceConsumptionFinder() {
  }

  @Override
  public Optional<ClusterResourceConsumtionDto> findByNameAndNamespace(
      String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace).map(cluster -> {
      ClusterResourceConsumtionDto status = new ClusterResourceConsumtionDto();

      try (KubernetesClient client = kubClientFactory.create()) {
        Optional<Pod> masterPod = client.pods()
            .inNamespace(cluster.getMetadata().getNamespace())
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(StackGresUserClusterContext.getClusterLabelMapper(cluster).clusterLabels())
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

        return status;
      }
    });
  }

  private List<String> exec(KubernetesClient client, Pod pod, String... args)
      throws Exception {
    return PodExec.exec(client, pod, Patroni.NAME, args);
  }

}
