/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import io.fabric8.kubernetes.client.Watcher;

public interface WatcherListener<T> {

  void eventReceived(Watcher.Action action, T resource);

  void watcherClosed(Exception ex);
}
