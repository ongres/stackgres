/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;

public interface PodWatcher {

  Uni<Pod> waitUntilIsReady(String clusterName, String name, String namespace);

  Uni<Void> waitUntilIsRemoved(String name, String namespace);

  Uni<Pod> waitUntilIsReplaced(Pod pod);

}
