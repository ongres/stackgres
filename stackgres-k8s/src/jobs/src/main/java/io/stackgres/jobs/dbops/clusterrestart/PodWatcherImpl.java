/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.ClusterPendingRestartUtil;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PodWatcherImpl implements PodWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(PodWatcherImpl.class);
  private final KubernetesClient client;

  @Inject
  public PodWatcherImpl(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Uni<Pod> waitUntilIsReady(String clusterName, String name, String namespace) {
    return waitUntilIsCreated(name, namespace)
        .chain(pod -> waitUntilReady(clusterName, pod));
  }

  private Uni<Pod> waitUntilReady(String clusterName, Pod pod) {
    String podName = pod.getMetadata().getName();
    String namespace = pod.getMetadata().getNamespace();

    return Uni.createFrom().emitter(em -> {
      Pod readyPod = null;
      do {
        Pod updatedPod = client.pods().inNamespace(namespace).withName(podName).get();
        LOGGER.info("Waiting for pod {} to be ready. Current state {}", podName,
            updatedPod.getStatus().getPhase());

        try {
          readyPod = client.pods().inNamespace(namespace).withName(podName).waitUntilReady(60,
              SECONDS);
          LOGGER.info("Pod {} ready!", podName);
          em.complete(readyPod);
        } catch (KubernetesClientTimeoutException timeoutException) {
          StackGresCluster sgCluster = getSgCluster(clusterName, namespace);
          if (sgCluster != null) {
            Optional<StatefulSet> sts = getStatefulSet(clusterName, namespace);
            RestartReasons restartReasons =
                ClusterPendingRestartUtil.getRestartReasons(
                    Optional.ofNullable(sgCluster.getStatus())
                    .map(StackGresClusterStatus::getPodStatuses)
                    .orElse(ImmutableList.of()),
                    sts, ImmutableList.of(updatedPod));
            if (restartReasons.getReasons().contains(RestartReason.STATEFULSET)) {
              String warningMessage = String.format("Statefulset for pod %s changed!", podName);
              LOGGER.info(warningMessage);
              em.fail(new RuntimeException(warningMessage));
            }
          }
        }
        waitForNextCheck();
      } while (readyPod == null);
    });
  }

  private void waitForNextCheck() {
    try {
      TimeUnit.SECONDS.sleep(15);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private StackGresCluster getSgCluster(String clusterName, String namespace) {
    return client.resources(StackGresCluster.class, StackGresClusterList.class)
        .inNamespace(namespace).withName(clusterName).get();
  }

  private Optional<StatefulSet> getStatefulSet(String clusterName, String namespace) {
    return Optional.ofNullable(client.apps().statefulSets()
        .inNamespace(namespace).withName(clusterName).get());
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
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .atMost(10);
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
