/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.MutinyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterInstanceManager {

  private static final String POD_NAME_FORMAT = "%s-%d";

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  @Inject
  LabelFactoryForCluster labelFactory;

  @Inject
  PodWatcher podWatcher;

  @Inject
  ResourceScanner<Pod> podScanner;

  @Inject
  DbOpsExecutorService executorService;

  public Uni<Pod> increaseClusterInstances(String name, String namespace) {
    return increaseInstances(name, namespace)
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("increasing instances"))
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

  public Uni<Void> decreaseClusterInstances(String name, String namespace) {
    return decreaseInstances(name, namespace)
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("decreasing instances"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely()
        .chain(podWatcher::waitUntilIsRemoved);
  }

  private Uni<Pod> decreaseInstances(String name, String namespace) {
    return getCluster(name, namespace)
        .chain(this::decreaseConfiguredInstances);
  }

  private Uni<StackGresCluster> getCluster(String name, String namespace) {
    return executorService.itemAsync(() -> {
      Optional<StackGresCluster> cluster = clusterFinder
          .findByNameAndNamespace(name, namespace);
      return cluster.orElseThrow(() -> new IllegalArgumentException(
          "SGCluster " + name + " not found in namespace" + namespace));
    });
  }

  private Uni<String> increaseConfiguredInstances(StackGresCluster cluster) {
    return executorService.itemAsync(() -> {
      String newPodName = getPodNameToBeCreated(cluster);
      int currentInstances = cluster.getSpec().getInstances();
      cluster.getSpec().setInstances(currentInstances + 1);
      clusterScheduler.update(cluster);
      return newPodName;
    });
  }

  private Uni<Pod> decreaseConfiguredInstances(StackGresCluster cluster) {
    return executorService.itemAsync(() -> {
      Pod podToBeDeleted = getPodToBeDeleted(cluster);
      int currentInstances = cluster.getSpec().getInstances();
      cluster.getSpec().setInstances(currentInstances - 1);
      clusterScheduler.update(cluster);
      return podToBeDeleted;
    });
  }

  private List<Pod> geClusterPods(StackGresCluster cluster) {
    Map<String, String> podLabels = labelFactory.clusterLabelsWithoutUidAndScope(cluster);
    final String namespace = cluster.getMetadata().getNamespace();
    return podScanner.getResourcesInNamespaceWithLabels(namespace, podLabels);
  }

  @SuppressWarnings("null")
  private String getPodNameToBeCreated(StackGresCluster cluster) {
    List<Pod> currentPods = geClusterPods(cluster);

    List<String> podNames = currentPods.stream()
        .map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .toList();

    List<Integer> podIndexes = podNames.stream()
        .map(podName -> Integer.parseInt(podName.substring(podName.lastIndexOf('-') + 1)))
        .sorted(Integer::compare)
        .toList();

    final int maxIndex = podIndexes.stream()
        .max(Integer::compare)
        .orElse(-1);
    final int prevMaxIndex = Seq.seq(podIndexes).zipWithIndex()
        .filter(t -> t.v1.intValue() == t.v2.intValue())
        .map(Tuple2::v1)
        .max(Integer::compare)
        .orElse(-1);

    final int newIndex;
    if (maxIndex >= podIndexes.size()) {
      newIndex = prevMaxIndex + 1;
    } else {
      newIndex = maxIndex + 1;
    }

    return String.format(POD_NAME_FORMAT, cluster.getMetadata().getName(), newIndex);
  }

  private Pod getPodToBeDeleted(StackGresCluster cluster) {
    List<Pod> currentPods = geClusterPods(cluster);

    List<Pod> replicas = currentPods.stream()
        .filter(pod -> PatroniUtil.REPLICA_ROLE.equals(
            pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)))
        .toList();

    if (replicas.isEmpty()) {
      return currentPods.stream()
          .filter(pod -> PatroniUtil.PRIMARY_ROLE.equals(
              pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY))
              || PatroniUtil.OLD_PRIMARY_ROLE.equals(
                  pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)))
          .findFirst()
          .orElseThrow(() -> new InvalidClusterException(
              "Cluster does not have a primary pod"));
    } else {
      return Seq.seq(replicas)
          .sorted(Comparator.comparing(
              replica -> replica.getMetadata().getName()))
          .findLast()
          .orElseThrow(() -> new InvalidClusterException(
              "Cluster does not have a replica pod"));
    }

  }
}
