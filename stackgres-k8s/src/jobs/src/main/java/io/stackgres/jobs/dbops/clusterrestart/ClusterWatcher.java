/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.Optional;

import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public interface ClusterWatcher {

  Uni<StackGresCluster> waitUntilIsReady(String name, String namespace);

  Uni<Optional<String>> getAvailablePrimary(String clusterName, String namespace);

}
