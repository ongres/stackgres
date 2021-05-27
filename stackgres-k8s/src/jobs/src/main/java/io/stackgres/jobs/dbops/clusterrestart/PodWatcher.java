/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.KubernetesClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PodWatcher implements Watcher<Pod> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PodWatcher.class);
  private final KubernetesClientFactory clientFactory;

  @Inject
  public PodWatcher(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Override
  public Uni<Pod> waitUntilIsReady(String name, String namespace) {

    return waitUntilIsCreated(name, namespace)
        .chain(this::waitUntilReady);

  }

  private Uni<Pod> waitUntilReady(Pod pod) {

    String podName = pod.getMetadata().getName();
    String namespace = pod.getMetadata().getNamespace();

    final Uni<Pod> podReadyPoll = Uni.createFrom().emitter(em -> {
      LOGGER.debug("Waiting for pod {} to be ready", podName);
      try (KubernetesClient client = clientFactory.create()) {
        var readyPod = client.pods().inNamespace(namespace).withName(podName)
            .waitUntilReady(30, TimeUnit.MINUTES);
        em.complete(readyPod);
      } catch (InterruptedException e) {
        em.fail(e);
      }
    });
    return podReadyPoll.onFailure().retry().indefinitely();
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
      var pod = clientFactory.withNewClient(
          client -> client.pods().inNamespace(namespace).withName(name).get());
      if (pod == null) {
        LOGGER.debug("Pod {} not found in namespace {}", name, namespace);
      } else {
        LOGGER.debug("Pod {} found in namespace {}", name, namespace);
      }
      em.complete(pod);
    });
  }

}
