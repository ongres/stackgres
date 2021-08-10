/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import org.immutables.value.Value;

@Value.Immutable
public interface ClusterRestartState {

  String getDbOpsName();

  String getDbOpsOperation();

  String getClusterName();

  String getNamespace();

  String getRestartMethod();

  boolean isOnlyPendingRestart();

  Pod getPrimaryInstance();

  boolean isSwitchoverInitiated();

  boolean isSwitchoverFinalized();

  List<Pod> getInitialInstances();

  List<Pod> getTotalInstances();

  List<Pod> getRestartedInstances();

  Map<Pod, RestartReasons> getPodRestartReasonsMap();

  @Value.Check
  default void check() {
    Preconditions.checkState(getTotalInstances().stream()
        .anyMatch(getPrimaryInstance()::equals));
    Preconditions.checkState(getInitialInstances().stream()
        .allMatch(initialInstance -> getTotalInstances().stream()
            .anyMatch(initialInstance::equals)));
    Preconditions.checkState(getRestartedInstances().stream()
        .allMatch(initialInstance -> getTotalInstances().stream()
            .anyMatch(initialInstance::equals)));
    Preconditions.checkState(getTotalInstances().size() == getPodRestartReasonsMap().size());
    Preconditions.checkState(getTotalInstances().stream()
        .allMatch(getPodRestartReasonsMap()::containsKey));
  }

  @Value.Derived
  default boolean hasToBeRestarted(Pod pod) {
    return !getRestartedInstances().contains(pod)
        && (!isOnlyPendingRestart()
            || getPodRestartReasonsMap().get(pod).requiresRestart());
  }

}
