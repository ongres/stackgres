/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.ClusterPendingRestartUtil;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import org.immutables.value.Value;

@Value.Immutable
public interface ClusterRestartState {

  StackGresDbOps getDbOps();

  StackGresCluster getCluster();

  Optional<StatefulSet> getStatefulSet();

  String getRestartMethod();

  boolean isOnlyPendingRestart();

  Pod getPrimaryInstance();

  boolean isSwitchoverInitiated();

  List<Pod> getInitialInstances();

  List<Pod> getTotalInstances();

  List<Pod> getRestartedInstances();

  @Value.Derived
  default String getDbOpsName() {
    return getDbOps().getMetadata().getName();
  }

  @Value.Derived
  default String getDbOpsOperation() {
    return getDbOps().getSpec().getOp();
  }

  @Value.Derived
  default String getClusterName() {
    return getDbOps().getSpec().getSgCluster();
  }

  @Value.Derived
  default String getNamespace() {
    return getDbOps().getMetadata().getNamespace();
  }

  @Value.Derived
  default boolean hasToBeRestarted(Pod pod) {
    return !getRestartedInstances().contains(pod)
        && (!isOnlyPendingRestart()
            || isPendingRestart(pod));
  }

  @Value.Derived
  default boolean isPendingRestart(Pod pod) {
    return getRestartReasons(pod).requiresRestart();
  }

  @Value.Derived
  default RestartReasons getRestartReasons(Pod pod) {
    return ClusterPendingRestartUtil.getRestartReasons(
        Optional.ofNullable(getCluster().getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .orElse(ImmutableList.of()),
        getStatefulSet(),
        ImmutableList.of(pod));
  }

}
