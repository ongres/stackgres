/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public interface ClusterWatcher {

  Uni<StackGresCluster> waitUntilIsReady(String name, String namespace);

  Uni<Boolean> isAvailable(String clusterName, String namespace, String podName);

}
