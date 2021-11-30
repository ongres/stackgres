/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.base.Joiner;
import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterWatcherImpl implements ClusterWatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterWatcherImpl.class);

  private final PatroniApiHandler patroniApiHandler;

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  private final ResourceScanner<Pod> podScanner;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public ClusterWatcherImpl(PatroniApiHandler patroniApiHandler,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      ResourceScanner<Pod> podScanner,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.patroniApiHandler = patroniApiHandler;
    this.labelFactory = labelFactory;
    this.podScanner = podScanner;
    this.clusterFinder = clusterFinder;
  }

  private static boolean isAllMembersReady(List<ClusterMember> members) {
    return members.stream().allMatch(ClusterWatcherImpl::isMemberReady);
  }

  private static boolean isMemberReady(ClusterMember member) {
    if (member.getRole() == MemberRole.LEADER) {
      final boolean ready = member.getState() == MemberState.RUNNING
          && member.getApiUrl().isPresent()
          && member.getPort().isPresent()
          && member.getTimeline().isPresent()
          && member.getHost().isPresent();
      if (!ready) {
        LOGGER.debug("Leader pod not ready, state: {}", member);
      }
      return ready;
    } else {
      final boolean ready = member.getState() == MemberState.RUNNING
          && member.getApiUrl().isPresent()
          && member.getPort().isPresent()
          && member.getTimeline().isPresent()
          && member.getHost().isPresent()
          && member.getLag().isPresent();
      if (!ready) {
        LOGGER.debug("Non leader pod not ready, state: {}", member);
      }
      return ready;
    }
  }

  public StackGresCluster findByNameAndNamespace(String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(() -> {
          LOGGER.info("SGCluster {} in namespace {} not found", name, namespace);
          return new IllegalStateException("cluster not found");
        });
  }

  @Override
  public Uni<StackGresCluster> waitUntilIsReady(String name, String namespace) {
    return Uni.createFrom().item(() -> findByNameAndNamespace(name, namespace))
        .call(cluster -> scanClusterPods(cluster)
            .chain(() -> getClusterMembers(cluster))
            .onFailure()
            .retry()
            .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
            .indefinitely());
  }

  private Uni<List<Pod>> scanClusterPods(StackGresCluster cluster) {
    return Uni.createFrom().item(() -> {
      var podsLabels = labelFactory.patroniClusterLabels(cluster);
      final String labelsAsString = Joiner.on(",").withKeyValueSeparator(":").join(podsLabels);
      LOGGER.debug("Scanning for pods of cluster {} with labels {}",
          cluster.getMetadata().getName(), labelsAsString);

      var pods = podScanner
          .findByLabelsAndNamespace(cluster.getMetadata().getNamespace(), podsLabels);

      int expectedInstances = cluster.getSpec().getInstances();

      if (expectedInstances == pods.size()) {
        return pods;
      } else {
        LOGGER.debug("Not all expected pods found with labels {}, expected {}, actual {}",
            labelsAsString,
            expectedInstances,
            pods.size());
        throw new InvalidCluster("No all pods found");
      }
    });
  }

  private Uni<List<ClusterMember>> getClusterMembers(StackGresCluster cluster) {
    final String name = cluster.getMetadata().getName();
    LOGGER.debug("Looking for cluster members of cluster {}", name);
    return patroniApiHandler.getClusterMembers(name,
        cluster.getMetadata().getNamespace())
        .onItem().transform(members -> {
          if (isAllMembersReady(members)) {
            LOGGER.debug("All members of cluster {} ready", name);
            return members;
          } else {
            var podsReady = members.stream().filter(ClusterWatcherImpl::isMemberReady)
                .map(ClusterMember::getName)
                .collect(Collectors.joining());
            var podsNotReady = members.stream().filter(m -> !isMemberReady(m))
                .map(ClusterMember::getName)
                .collect(Collectors.joining());
            LOGGER.debug("Not all pods are ready. Pods not ready: {}, Pods ready: {}",
                podsNotReady, podsReady);
            throw new InvalidCluster("Not all pods are ready");
          }
        });
  }

  @Override
  public Uni<Boolean> isAvailable(String clusterName, String namespace, String podName) {
    return patroniApiHandler.getClusterMembers(clusterName, namespace)
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .atMost(10)
        .onItemOrFailure()
        .transform((members, failure) -> {
          return failure != null && members.stream()
              .anyMatch(member -> member.getName().equals(podName)
                  && member.getState().equals(MemberState.RUNNING));
        });
  }

}
