/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;

public interface DatabaseOperationJob {

  Uni<ClusterRestartState> runJob(StackGresDbOps dbOps, StackGresCluster cluster);

}
