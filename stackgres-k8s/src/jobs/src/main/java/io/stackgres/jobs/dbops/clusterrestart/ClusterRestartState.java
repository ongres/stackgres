/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import org.immutables.value.Value;

@Value.Immutable
public interface ClusterRestartState {

  String getRestartMethod();

  boolean isOnlyPendingRrestart();

  String getClusterName();

  String getNamespace();

  List<StackGresClusterPodStatus> getPodStatuses();

  Optional<StatefulSet> getStatefulSet();

  Pod getPrimaryInstance();

  boolean isSwitchoverInitiated();

  List<Pod> getInitialInstances();

  List<Pod> getTotalInstances();

  List<Pod> getRestartedInstances();

}
