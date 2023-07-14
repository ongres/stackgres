/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.jobs.dbops.MutinyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PodRestartImpl implements PodRestart {

  private final ResourceWriter<Pod> podWriter;

  private final PodWatcher podWatcher;

  @Inject
  public PodRestartImpl(ResourceWriter<Pod> podWriter, PodWatcher podWatcher) {
    this.podWriter = podWriter;
    this.podWatcher = podWatcher;
  }

  @Override
  public Uni<Pod> restartPod(String name, Pod pod) {
    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    return podWatcher.waitUntilIsCreated(podName, podNamespace)
        .onItem()
        .invoke(podWriter::delete)
        .chain(podWatcher::waitUntilIsReplaced)
        .chain(() -> podWatcher.waitUntilIsReady(name, podName, podNamespace, true))
        .onFailure(StatefulSetChangedException.class::isInstance)
        .retry().indefinitely()
        .onFailure()
        .transform(ex -> MutinyUtil.logOnFailureToRetry(ex,
            "restarting pod {}", pod.getMetadata().getName()))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .atMost(10);
  }

}
