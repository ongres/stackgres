/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterResourceWatcherFactory {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ClusterResourceWatcherFactory.class);

  private final ClusterReconciliationCycle clusterReconciliationCycle;
  private final EventController eventController;

  /**
   * Create a {@code StackGresClusterWatcherFactory} instance.
   */
  @Inject
  public ClusterResourceWatcherFactory(
      ClusterReconciliationCycle clusterReconciliationCycle,
      EventController eventController) {
    super();
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.eventController = eventController;
  }

  public <T extends HasMetadata> Watcher<T> createWatcher() {
    return new WatcherInstance<>();
  }

  private class WatcherInstance<T extends HasMetadata> implements Watcher<T> {

    @Override
    public void eventReceived(Action action, T resource) {
      LOGGER.debug("Action <{}> on resource: [{}] {}.{}", action, resource.getKind(),
          resource.getMetadata().getNamespace(), resource.getMetadata().getName());
      try {
        switch (action) {
          case ADDED:
          case DELETED:
          case MODIFIED:
            clusterReconciliationCycle.reconcile();
            break;
          default:
            throw new UnsupportedOperationException("Action not supported: " + action);
        }
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
        new Thread(() -> System.exit(1)).start();
      }
    }
  }

}
