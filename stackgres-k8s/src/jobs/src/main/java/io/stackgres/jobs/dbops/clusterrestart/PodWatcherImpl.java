/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.ClusterPendingRestartUtil;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.jobs.dbops.MutinyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PodWatcherImpl implements PodWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(PodWatcherImpl.class);

  @Inject
  ResourceFinder<Pod> podFinder;

  @Inject
  ResourceFinder<StatefulSet> statefulSetFinder;

  @Override
  public Uni<Pod> waitUntilIsReady(String clusterName, String name, String namespace,
      boolean checkStatefulSetChanges) {
    return waitUntilIsCreated(name, namespace)
        .chain(pod -> waitUntilReady(clusterName, pod, checkStatefulSetChanges));
  }

  private Uni<Pod> waitUntilReady(String clusterName, Pod pod, boolean checkStatefulSetChanges) {
    String name = pod.getMetadata().getName();
    String namespace = pod.getMetadata().getNamespace();

    return findPod(name, namespace)
        .onItem()
        .transform(updatedPod -> updatedPod
            .orElseThrow(() -> new RuntimeException("Pod " + name + " not found")))
        .chain(updatedPod -> Uni.createFrom().item(() -> {
          LOGGER.info("Waiting for pod {} to be ready. Current state {}", name,
              updatedPod.getStatus().getPhase());
          if (!Readiness.getInstance().isReady(updatedPod)) {
            throw Optional.of(checkStatefulSetChanges)
                .filter(check -> check)
                .flatMap(check -> getStatefulSetChangedException(
                    clusterName, name, namespace, updatedPod))
                .map(RuntimeException.class::cast)
                .orElse(new RuntimeException("Pod " + name + " not ready"));
          }
          LOGGER.info("Pod {} ready!", name);
          return updatedPod;
        }))
        .onFailure()
        .transform(ex -> MutinyUtil.logOnFailureToRetry(ex,
            "waiting for Pod {} to be ready", name))
        .onFailure(failure -> !(failure instanceof StatefulSetChangedException))
        .retry()
        .withBackOff(Duration.ofSeconds(2), Duration.ofSeconds(60))
        .indefinitely();
  }

  private Optional<StatefulSetChangedException> getStatefulSetChangedException(String clusterName,
      String podName, String namespace, Pod updatedPod) {
    Optional<StatefulSet> sts = getStatefulSet(clusterName, namespace);
    RestartReasons restartReasons =
        ClusterPendingRestartUtil.getRestartReasons(
            ImmutableList.of(), sts, ImmutableList.of(updatedPod));
    if (restartReasons.getReasons().contains(RestartReason.STATEFULSET)) {
      String warningMessage = String.format(
          "Statefulset for pod %s changed!", podName);
      LOGGER.info(warningMessage);
      return Optional.of(new StatefulSetChangedException(warningMessage));
    }
    return Optional.empty();
  }

  private Optional<StatefulSet> getStatefulSet(String clusterName, String namespace) {
    return statefulSetFinder.findByNameAndNamespace(clusterName, namespace);
  }

  @Override
  public Uni<Pod> waitUntilIsCreated(String name, String namespace) {
    LOGGER.debug("Waiting for pod {} to be created", name);

    return findPod(name, namespace)
        .onItem()
        .transform(pod -> pod
            .orElseThrow(() -> new RuntimeException("Pod " + name + " not found")))
        .onFailure()
        .transform(ex -> MutinyUtil.logOnFailureToRetry(ex,
            "searching for pod {}", name))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .indefinitely();
  }

  @Override
  public Uni<Void> waitUntilIsRemoved(String name, String namespace) {
    return findPod(name, namespace)
        .onItem()
        .invoke(removedPod -> removedPod
            .ifPresent(pod -> {
              throw new RuntimeException("Pod " + name + " not removed");
            }))
        .onFailure()
        .transform(ex -> MutinyUtil.logOnFailureToRetry(ex,
            "deleting Pod {}", name))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .indefinitely()
        .onItem()
        .<Void>transform(item -> null);

  }

  @Override
  public Uni<Pod> waitUntilIsReplaced(Pod pod) {
    String oldCreationTimestamp = pod.getMetadata().getCreationTimestamp();
    String name = pod.getMetadata().getName();
    String namespace = pod.getMetadata().getNamespace();
    return findPod(name, namespace)
        .onItem()
        .transform(newPod -> newPod
            .orElseThrow(() -> new RuntimeException("Pod " + name + " not found")))
        .onItem()
        .transform(newPod -> {
          String newCreationTimestamp = newPod.getMetadata().getCreationTimestamp();
          if (Objects.equals(oldCreationTimestamp, newCreationTimestamp)) {
            throw new RuntimeException("Pod " + name + " not replaced");
          } else {
            return newPod;
          }
        })
        .onFailure()
        .transform(ex -> MutinyUtil.logOnFailureToRetry(ex,
            "waiting for Pod {} to be replaced", name))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .indefinitely();
  }

  private Uni<Optional<Pod>> findPod(String name, String namespace) {
    return Uni.createFrom().item(() -> podFinder.findByNameAndNamespace(name, namespace))
        .onItem()
        .invoke(pod -> {
          if (pod.isEmpty()) {
            LOGGER.debug("Pod {} not found in namespace {}", name, namespace);
          } else {
            LOGGER.debug("Pod {} found in namespace {}", name, namespace);
          }
        });
  }

}
