/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.Watcher.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractResourceWatcherFactory {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractResourceWatcherFactory.class);

  public <T extends HasMetadata> Watcher<T> createWatcher(Consumer<Action> actionConsumer) {
    return new WatcherInstance<>(actionConsumer, new EmptyWatcherListener<>());
  }

  public <T extends HasMetadata> Watcher<T> createWatcher(Consumer<Action> actionConsumer,
      WatcherListener<T> watcherListener) {
    return new WatcherInstance<>(actionConsumer, watcherListener);
  }

  private class WatcherInstance<T extends HasMetadata> implements Watcher<T> {

    private final Consumer<Action> actionConsumer;
    private final WatcherListener<T> watcherListener;

    public WatcherInstance(Consumer<Action> actionConsumer, WatcherListener<T> watcherListener) {
      super();
      this.actionConsumer = actionConsumer;
      this.watcherListener = watcherListener;
    }

    @Override
    public void eventReceived(Action action, T resource) {
      LOGGER.debug("Action <{}> on resource: [{}] {}.{}", action, resource.getKind(),
          resource.getMetadata().getNamespace(), resource.getMetadata().getName());
      try {
        actionConsumer.accept(action);
        watcherListener.eventReceived(action, resource);
      } catch (Exception ex) {
        LOGGER.error("Error while performing action: <{}>", action, ex);
      }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
      if (cause == null) {
        LOGGER.info("onClose was called");
        AbstractResourceWatcherFactory.this.onClose();
      } else {
        LOGGER.error("onClose was called, ", cause);
        AbstractResourceWatcherFactory.this.onError(cause);
        watcherListener.watcherError(cause);
      }
    }
  }

  protected abstract void onError(KubernetesClientException cause);

  protected abstract void onClose();

  public static class EmptyWatcherListener<T> implements WatcherListener<T> {
    @Override
    public void eventReceived(Action action, T resource) {}

    @Override
    public void watcherError(Exception ex) {}

    @Override
    public void watcherClosed() {}
  }

}
