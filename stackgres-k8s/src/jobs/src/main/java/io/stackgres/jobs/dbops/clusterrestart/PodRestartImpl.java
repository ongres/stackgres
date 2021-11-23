/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.resource.ResourceWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PodRestartImpl implements PodRestart {

  private static final Logger LOGGER = LoggerFactory.getLogger(PodRestartImpl.class);
  private final ResourceWriter<Pod> podWriter;

  private final Watcher<Pod> podWatcher;

  @Inject
  public PodRestartImpl(ResourceWriter<Pod> podWriter, Watcher<Pod> podWatcher) {
    this.podWriter = podWriter;
    this.podWatcher = podWatcher;
  }

  @Override
  public Uni<Pod> restartPod(Pod pod) {

    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    return deletePod(pod)
        .chain(() -> podWatcher.waitUntilIsReplaced(pod))
        .chain(() -> podWatcher.waitUntilIsReady(podName, podNamespace))
        .onFailure().retry().atMost(10);
  }

  public Uni<Void> deletePod(Pod pod) {
    LOGGER.info("Restarting pod {}", pod.getMetadata().getName());
    return Uni.createFrom().emitter(em -> {
      podWriter.delete(pod);
      em.complete(null);
    });
  }
}
