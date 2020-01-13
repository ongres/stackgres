/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.quarkus.runtime.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterResourceWatcherFactory {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ClusterResourceWatcherFactory.class);

  private final EventController eventController;

  /**
   * Create a {@code StackGresClusterWatcherFactory} instance.
   */
  @Inject
  public ClusterResourceWatcherFactory(
      EventController eventController) {
    super();
    this.eventController = eventController;
  }

  public <T extends HasMetadata> Watcher<T> createWatcher(Consumer<Action> actionConsumer) {
    return new WatcherInstance<>(actionConsumer);
  }

  private class WatcherInstance<T extends HasMetadata> implements Watcher<T> {

    private final Consumer<Action> actionConsumer;

    public WatcherInstance(Consumer<Action> actionConsumer) {
      super();
      this.actionConsumer = actionConsumer;
    }

    @Override
    public void eventReceived(Action action, T resource) {
      LOGGER.debug("Action <{}> on resource: [{}] {}.{}", action, resource.getKind(),
          resource.getMetadata().getNamespace(), resource.getMetadata().getName());
      try {
        actionConsumer.accept(action);
      } catch (Exception ex) {
        LOGGER.error("Error while performing action: <{}>", action, ex);
      }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
      if (cause == null) {
        LOGGER.info("onClose was called");
      } else {
        LOGGER.error("onClose was called, ", cause);
        eventController.sendEvent(EventReason.OPERATOR_ERROR,
            "Watcher was closed unexpectedly: " + (cause != null && cause.getMessage() != null
            ? cause.getMessage() : "unknown reason"));
        new Thread(() -> Application.currentApplication().stop()).start();
      }
    }
  }

}
