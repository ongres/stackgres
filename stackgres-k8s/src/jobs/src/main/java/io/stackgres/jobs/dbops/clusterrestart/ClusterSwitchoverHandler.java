/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.concurrent.CompletableFuture;

import io.smallrye.mutiny.Uni;

public interface ClusterSwitchoverHandler {

  Uni<Void> performSwitchover(String clusterName, String clusterNamespace);

}
