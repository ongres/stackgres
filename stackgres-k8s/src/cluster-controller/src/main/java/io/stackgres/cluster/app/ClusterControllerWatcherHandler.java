/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

public interface ClusterControllerWatcherHandler {
  void startWatchers();

  void stopWatchers();
}
