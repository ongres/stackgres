/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import io.fabric8.kubernetes.api.model.Pod;
import io.smallrye.mutiny.Uni;

public interface PodRestart {

  Uni<Pod> restartPod(String name, Pod pod);
}
