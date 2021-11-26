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
public class ClusterWatcher implements Watcher<StackGresCluster> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterWatcher.class);

  private final PatroniApiHandler patroniApiHandler;

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  private final ResourceScanner<Pod> podScanner;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public ClusterWatcher(PatroniApiHandler patroniApiHandler,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      ResourceScanner<Pod> podScanner,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.patroniApiHandler = patroniApiHandler;
    this.labelFactory = labelFactory;
    this.podScanner = podScanner;
    this.clusterFinder = clusterFinder;
  }

  private static boolean isAllMembersReady(List<ClusterMember> members) {
    return members.stream().allMatch(ClusterWatcher::isMemberReady);
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
    return Uni.createFrom().emitter(em -> {
      LOGGER.debug("Looking for SGCluster {} in namespace {}", name, namespace);
      StackGresCluster cluster = findByNameAndNamespace(name, namespace);
      scanClusterPods(cluster)
          .chain(() -> getClusterMembers(cluster))
          .onFailure()
          .retry()
          .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
          .indefinitely()
          .subscribe().with(members -> em.complete(cluster));
    });
  }

  private Uni<List<Pod>> scanClusterPods(StackGresCluster cluster) {
    return Uni.createFrom().emitter(em -> {
      var podsLabels = labelFactory.patroniClusterLabels(cluster);
      final String labelsAsString = Joiner.on(",").withKeyValueSeparator(":").join(podsLabels);
      LOGGER.debug("Scanning for pods of cluster {} with labels {}",
          cluster.getMetadata().getName(), labelsAsString);

      var pods = podScanner
          .findByLabelsAndNamespace(cluster.getMetadata().getNamespace(), podsLabels);

      int expectedInstances = cluster.getSpec().getInstances();

      if (expectedInstances == pods.size()) {
        em.complete(pods);
      } else {
        LOGGER.debug("Not all expected pods found with labels {}, expected {}, actual {}",
            labelsAsString,
            expectedInstances,
            pods.size());
        em.fail(new InvalidCluster("No all pods found"));
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
            var podsReady = members.stream().filter(ClusterWatcher::isMemberReady)
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
  public Uni<Void> waitUntilIsRemoved(String name, String namespace) {
    throw new UnsupportedOperationException();
  }
}
