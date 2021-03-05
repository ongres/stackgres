/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

public interface WatcherListener<T> {

  void eventReceived(Watcher.Action action, T resource);

  void watcherError(WatcherException ex);

  void watcherClosed();

}
