/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.MutinyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterWatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterWatcher.class);

  @Inject
  PatroniApiHandler patroniApiHandler;

  @Inject
  LabelFactoryForCluster labelFactory;

  @Inject
  ResourceScanner<Pod> podScanner;

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  DbOpsExecutorService executorService;

  private static boolean isPrimaryReady(List<PatroniMember> members) {
    return members.stream().anyMatch(ClusterWatcher::isPrimaryReady);
  }

  private static boolean isPrimaryReady(PatroniMember member) {
    if (member.isPrimary()) {
      final boolean ready = member.isRunning()
          && member.getTimeline() != null
          && member.getHost() != null;
      if (!ready) {
        LOGGER.debug("Leader pod not ready, state: {}", member);
      }
      return ready;
    } else {
      final boolean ready = member.isRunning()
          && member.getTimeline() != null
          && member.getHost() != null
          && member.getLagInMb() != null;
      if (!ready) {
        LOGGER.debug("Non leader pod not ready, state: {}", member);
      }
      return false;
    }
  }

  public StackGresCluster findByNameAndNamespace(String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(() -> {
          LOGGER.info("SGCluster {} in namespace {} not found", name, namespace);
          return new IllegalStateException("cluster not found");
        });
  }

  public Uni<StackGresCluster> waitUntilIsReady(String name, String namespace) {
    return executorService.itemAsync(() -> findByNameAndNamespace(name, namespace))
        .call(cluster -> scanClusterPods(cluster)
            .chain(() -> getClusterMembers(cluster))
            .onFailure()
            .transform(MutinyUtil.logOnFailureToRetry("scanning cluster and Pods"))
            .onFailure()
            .retry()
            .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
            .indefinitely());
  }

  private Uni<List<Pod>> scanClusterPods(StackGresCluster cluster) {
    return executorService.itemAsync(() -> {
      var podsLabels = labelFactory.clusterLabelsWithoutUidAndScope(cluster);
      final String labelsAsString = Joiner.on(",").withKeyValueSeparator(":").join(podsLabels);
      LOGGER.debug("Scanning for pods of cluster {} with labels {}",
          cluster.getMetadata().getName(), labelsAsString);

      var pods = podScanner
          .getResourcesInNamespaceWithLabels(cluster.getMetadata().getNamespace(), podsLabels);

      int expectedInstances = cluster.getSpec().getInstances();

      if (expectedInstances == pods.size()) {
        return pods;
      } else {
        LOGGER.debug("Not all expected pods found with labels {}, expected {}, actual {}",
            labelsAsString,
            expectedInstances,
            pods.size());
        throw new InvalidClusterException("No all pods found");
      }
    });
  }

  private Uni<List<PatroniMember>> getClusterMembers(StackGresCluster cluster) {
    final String name = cluster.getMetadata().getName();
    LOGGER.debug("Looking for cluster members of cluster {}", name);
    return patroniApiHandler.getClusterMembers(name,
        cluster.getMetadata().getNamespace())
        .onItem()
        .transform(members -> {
          if (isPrimaryReady(members)) {
            LOGGER.debug("Primary of cluster {} ready", name);
            return members;
          } else {
            var primaryNotReady = members.stream()
                .filter(Predicate.not(ClusterWatcher::isPrimaryReady))
                .map(PatroniMember::getMember)
                .collect(Collectors.joining());
            LOGGER.debug("Primary {} is not ready",
                primaryNotReady);
            throw new InvalidClusterException("Primary is not ready");
          }
        });
  }

  public Uni<Optional<String>> getAvailablePrimary(String clusterName, String namespace) {
    return patroniApiHandler.getClusterMembers(clusterName, namespace)
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("retrieving cluster members"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .atMost(10)
        .onItemOrFailure()
        .transform((members, failure) -> Optional.ofNullable(members)
            .filter(m -> failure == null)
            .stream()
            .flatMap(List::stream)
            .filter(member -> member.isPrimary() && member.isRunning())
            .map(PatroniMember::getMember)
            .findAny());
  }

}
