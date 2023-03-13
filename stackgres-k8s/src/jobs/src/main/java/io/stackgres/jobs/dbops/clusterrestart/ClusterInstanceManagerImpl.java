/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceScanner;

@ApplicationScoped
public class ClusterInstanceManagerImpl implements ClusterInstanceManager {

  private static final String POD_NAME_FORMAT = "%s-%d";

  @Inject
  CustomResourceFinder<StackGresCluster> resourceFinder;

  @Inject
  CustomResourceScheduler<StackGresCluster> resourceScheduler;

  @Inject
  LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  PodWatcher podWatcher;

  @Inject
  ResourceScanner<Pod> podScanner;

  @Override
  public Uni<Pod> increaseClusterInstances(String name, String namespace) {
    return increaseInstances(name, namespace)
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely()
        .chain(newPodName -> podWatcher.waitUntilIsReady(name, newPodName, namespace, false));
  }

  private Uni<String> increaseInstances(String name, String namespace) {
    return getCluster(name, namespace)
        .chain(this::increaseConfiguredInstances);
  }

  @Override
  public Uni<Void> decreaseClusterInstances(String name, String namespace) {
    return decreaseInstances(name, namespace)
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely()
        .chain(podToBeDeleted -> podWatcher.waitUntilIsRemoved(podToBeDeleted, namespace));
  }

  private Uni<String> decreaseInstances(String name, String namespace) {
    return getCluster(name, namespace)
        .chain(this::decreaseConfiguredInstances);
  }

  private Uni<StackGresCluster> getCluster(String name, String namespace) {
    return Uni.createFrom().item(() -> {
      Optional<StackGresCluster> cluster = resourceFinder.findByNameAndNamespace(name, namespace);
      return cluster.orElseThrow(() -> new IllegalArgumentException(
          "SGCluster " + name + " not found in namespace" + namespace));
    });
  }

  private Uni<String> increaseConfiguredInstances(StackGresCluster cluster) {
    return Uni.createFrom().item(() -> {
      String newPodName = getPodNameToBeCreated(cluster);
      int currentInstances = cluster.getSpec().getInstances();
      cluster.getSpec().setInstances(currentInstances + 1);
      resourceScheduler.update(cluster);
      return newPodName;
    });
  }

  private Uni<String> decreaseConfiguredInstances(StackGresCluster cluster) {
    return Uni.createFrom().item(() -> {
      String podToBeDeleted = getPodToBeDeleted(cluster);
      int currentInstances = cluster.getSpec().getInstances();
      cluster.getSpec().setInstances(currentInstances - 1);
      resourceScheduler.update(cluster);
      return podToBeDeleted;
    });
  }

  private List<Pod> geClusterPods(StackGresCluster cluster) {
    Map<String, String> podLabels = labelFactory.patroniClusterLabels(cluster);
    final String namespace = cluster.getMetadata().getNamespace();
    return podScanner.findByLabelsAndNamespace(namespace, podLabels);
  }

  private String getPodNameToBeCreated(StackGresCluster cluster) {
    List<Pod> currentPods = geClusterPods(cluster);

    List<String> podNames = currentPods.stream()
        .map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .collect(Collectors.toUnmodifiableList());

    List<Integer> podIndexes = podNames.stream()
        .map(podName -> Integer.parseInt(podName.substring(podName.lastIndexOf('-') + 1)))
        .sorted(Integer::compare)
        .collect(Collectors.toList());

    int maxIndex = podIndexes.stream().max(Integer::compare).orElse(-1);

    if (maxIndex > podIndexes.size()) {
      Collections.sort(podIndexes);
      maxIndex = podIndexes.get(podIndexes.size() - 2);
    }
    int newMaxIndex = maxIndex + 1;

    return String.format(POD_NAME_FORMAT, cluster.getMetadata().getName(), newMaxIndex);
  }

  private String getPodToBeDeleted(StackGresCluster cluster) {
    List<Pod> currentPods = geClusterPods(cluster);

    List<Pod> replicas = currentPods.stream().filter(pod -> {
      String role = pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY);
      return PatroniUtil.REPLICA_ROLE.equals(role);
    }).collect(Collectors.toUnmodifiableList());

    if (replicas.isEmpty()) {
      return currentPods.stream().filter(pod -> {
        String role = pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY);
        return PatroniUtil.PRIMARY_ROLE.equals(role);
      }).findFirst().orElseThrow(() -> new InvalidClusterException(
          "Cluster does not have a primary pod"))
          .getMetadata().getName();
    } else {
      List<String> replicaNames = replicas.stream()
          .map(replica -> replica.getMetadata().getName())
          .sorted(String::compareTo)
          .collect(Collectors.toUnmodifiableList());

      return replicaNames.get(replicaNames.size() - 1);
    }

  }
}
