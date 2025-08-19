/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.ClusterRolloutUtil;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.MutinyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PodWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(PodWatcher.class);

  @Inject
  ResourceFinder<Pod> podFinder;

  @Inject
  ResourceFinder<StatefulSet> statefulSetFinder;

  @Inject
  DbOpsExecutorService executorService;

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
        .chain(updatedPod -> executorService.itemAsync(() -> {
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
    Optional<StatefulSet> statefulSet = getStatefulSet(clusterName, namespace);
    if (ClusterRolloutUtil.isStatefulSetPodPendingRestart(statefulSet, updatedPod)) {
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

  public Uni<Void> waitUntilIsRemoved(Pod removedPod) {
    return findPod(removedPod.getMetadata().getName(), removedPod.getMetadata().getNamespace())
        .onItem()
        .invoke(foundPod -> foundPod
            .filter(pod -> pod.getMetadata().getUid().equals(removedPod.getMetadata().getUid()))
            .ifPresent(pod -> {
              throw new RuntimeException("Pod " + removedPod.getMetadata().getName()
                  + " with uid " + removedPod.getMetadata().getUid() + " not removed");
            }))
        .onFailure()
        .transform(ex -> MutinyUtil.logOnFailureToRetry(ex,
            "deleting Pod {}", removedPod.getMetadata().getName()))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .indefinitely()
        .onItem()
        .<Void>transform(item -> null);
  }

  public Uni<Pod> waitUntilIsReplaced(Pod pod) {
    String oldUid = pod.getMetadata().getUid();
    String name = pod.getMetadata().getName();
    String namespace = pod.getMetadata().getNamespace();
    return findPod(name, namespace)
        .onItem()
        .transform(newPod -> newPod
            .orElseThrow(() -> new RuntimeException("Pod " + name + " not found")))
        .onItem()
        .transform(newPod -> {
          String newUid = newPod.getMetadata().getUid();
          if (Objects.equals(oldUid, newUid)) {
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
    return executorService.itemAsync(() -> podFinder.findByNameAndNamespace(name, namespace))
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
