/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;

import io.fabric8.kubernetes.api.model.Pod;
import org.immutables.value.Value;

@Value.Immutable
public interface ClusterRestartState {

  String getRestartMethod();

  String getClusterName();

  String getNamespace();

  Pod getPrimaryInstance();

  boolean isSwitchoverInitiated();

  List<Pod> getInitialInstances();

  List<Pod> getTotalInstances();

  List<Pod> getRestartedInstances();

}
