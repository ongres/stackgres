/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;

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
        .transform(failure -> {
          LOGGER.info("Error while restarting pod {}: {}",
              pod.getMetadata().getName(), failure.getMessage());
          return failure;
        })
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .atMost(10);
  }

}
