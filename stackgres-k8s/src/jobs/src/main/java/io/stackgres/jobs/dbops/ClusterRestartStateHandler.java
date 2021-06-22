/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;

public interface ClusterRestartStateHandler {

  Uni<StackGresDbOps> restartCluster(StackGresDbOps op);

}
