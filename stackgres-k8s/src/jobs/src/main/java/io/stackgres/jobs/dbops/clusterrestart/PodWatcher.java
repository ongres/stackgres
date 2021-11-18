/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PodWatcher implements Watcher<Pod> {

  public static final String UNKNOWN_POD_STATUS_PHASE = "Unknown";
  public static final String FAILED_POD_STATUS_PHASE = "Failed";
  public static final String RUNNING_POD_STATUS_PHASE = "Running";
  public static final String PENDING_POD_STATUS_PHASE = "Pending";
  private static final Logger LOGGER = LoggerFactory.getLogger(PodWatcher.class);
  private final KubernetesClient client;

  @Inject
  public PodWatcher(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Uni<Pod> waitUntilIsReady(String name, String namespace) {
    return waitUntilIsCreated(name, namespace)
        .chain(this::waitUntilReady);
  }

  private Uni<Pod> waitUntilReady(Pod pod) {
    String podName = pod.getMetadata().getName();
    String namespace = pod.getMetadata().getNamespace();

    Uni<Pod> podReadyPoll = Uni.createFrom().emitter(em -> {
      do {
        var updatedPod = client.pods().inNamespace(namespace).withName(podName).get();
        String updatedPodPhaseStatus = updatedPod.getStatus().getPhase();
        LOGGER.info("Waiting for pod {} to be ready. Current state {}", podName,
            updatedPodPhaseStatus);
        if (updatedPodPhaseStatus.equals(RUNNING_POD_STATUS_PHASE)) {
          LOGGER.info("Pod {} ready!", podName);
          em.complete(pod);
          break;
        }

        var warningEvent = checkForPodWarningEvent(pod, namespace);
        if (warningEvent.isPresent()) {
          LOGGER.info("Warning event from pod {} found!", podName);
          em.complete(null);
          break;
        }

        if (updatedPodPhaseStatus.equals(FAILED_POD_STATUS_PHASE)
            || updatedPodPhaseStatus.equals(UNKNOWN_POD_STATUS_PHASE)) {
          LOGGER.info("Pod {} with status {}", podName, updatedPodPhaseStatus);
          em.complete(null);
          break;
        }

        waitForNextCheck();

      } while (true);
    });
    return podReadyPoll.onFailure().retry()
        .withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10)).atMost(5);
  }

  private void waitForNextCheck() {
    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private Optional<Event> checkForPodWarningEvent(Pod pod, String namespace) {
    var warningEvent = client.v1().events().inNamespace(namespace)
        .withInvolvedObject(ResourceUtil.getObjectReference(pod))
        .list()
        .getItems()
        .stream()
        .filter(event -> event.getType().equals("Warning"))
        .findAny();
    return warningEvent;
  }

  protected Uni<Pod> waitUntilIsCreated(String name, String namespace) {
    LOGGER.debug("Waiting for pod {} to be created", name);

    return getPod(name, namespace)
        .onItem()
        .transform(pod -> {
          if (pod != null) {
            return pod;
          } else {
            throw new RuntimeException("Pod " + name + " not found");
          }
        })
        .onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(2))
        .expireIn(30 * 1000);
  }

  @Override
  public Uni<Void> waitUntilIsRemoved(String name, String namespace) {

    return getPod(name, namespace)
        .onItem().transformToUni(pod -> {
          if (pod != null) {
            return Uni.createFrom().failure(new RuntimeException("Pod " + name + " not removed"));
          } else {
            return Uni.createFrom().voidItem();
          }
        }).onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(5))
        .indefinitely();

  }

  @Override
  public Uni<Pod> waitUntilIsReplaced(Pod oldPod) {
    String oldCreationTimestamp = oldPod.getMetadata().getCreationTimestamp();
    String podName = oldPod.getMetadata().getName();
    String podNamespace = oldPod.getMetadata().getNamespace();
    return getPod(podName, podNamespace)
        .onItem().transform(pod -> {
          if (pod != null) {
            return pod;
          } else {
            throw new RuntimeException("Pod " + podName + " not found");
          }
        })
        .onItem().transform(newPod -> {
          String newCreationTimestamp = newPod.getMetadata().getCreationTimestamp();
          if (Objects.equals(oldCreationTimestamp, newCreationTimestamp)) {
            throw new RuntimeException("Pod " + podName + " not replaced");
          } else {
            return newPod;
          }
        }).onFailure()
        .retry()
        .withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(5))
        .indefinitely();
  }

  private Uni<Pod> getPod(String name, String namespace) {
    return Uni.createFrom().emitter(em -> {
      var pod = client.pods().inNamespace(namespace).withName(name).get();
      if (pod == null) {
        LOGGER.debug("Pod {} not found in namespace {}", name, namespace);
      } else {
        LOGGER.debug("Pod {} found in namespace {}", name, namespace);
      }
      em.complete(pod);
    });
  }

}
