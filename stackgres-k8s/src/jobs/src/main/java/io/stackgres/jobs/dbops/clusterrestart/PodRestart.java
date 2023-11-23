/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.MutinyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PodRestart {

  @Inject
  ResourceWriter<Pod> podWriter;

  @Inject
  PodWatcher podWatcher;

  @Inject
  DbOpsExecutorService executorService;

  public Uni<Pod> restartPod(String name, Pod pod) {
    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    return podWatcher.waitUntilIsCreated(podName, podNamespace)
        .chain(() -> executorService.invokeAsync(() -> podWriter.delete(pod)))
        .chain(() -> podWatcher.waitUntilIsReplaced(pod))
        .chain(() -> podWatcher.waitUntilIsReady(name, podName, podNamespace, true))
        .onFailure(StatefulSetChangedException.class::isInstance)
        .retry()
        .indefinitely()
        .onFailure()
        .transform(ex -> MutinyUtil.logOnFailureToRetry(ex,
            "restarting pod {}", pod.getMetadata().getName()))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .atMost(10);
  }

}
