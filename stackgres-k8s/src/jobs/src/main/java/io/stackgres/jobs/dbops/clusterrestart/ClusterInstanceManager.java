/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;

public interface ClusterInstanceManager {

  Uni<Pod> increaseClusterInstances(String name, String namespace);

  Uni<Void> decreaseClusterInstances(String name, String namespace);
}
