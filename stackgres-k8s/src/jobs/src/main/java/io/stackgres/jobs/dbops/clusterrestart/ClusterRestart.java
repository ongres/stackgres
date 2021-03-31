/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import io.smallrye.mutiny.Multi;

public interface ClusterRestart {

  Multi<RestartEvent> restartCluster(ClusterRestartState clusterState);
}
