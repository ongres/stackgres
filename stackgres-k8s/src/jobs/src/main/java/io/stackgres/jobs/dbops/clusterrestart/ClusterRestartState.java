/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.ClusterRolloutUtil.RestartReasons;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsOperation;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(visibility = ImplementationVisibility.PACKAGE)
public interface ClusterRestartState {

  String getDbOpsName();

  DbOpsOperation getDbOpsOperation();

  String getClusterName();

  String getNamespace();

  DbOpsMethodType getRestartMethod();

  boolean isOnlyPendingRestart();

  Optional<String> getPrimaryInstance();

  boolean isSwitchoverInitiated();

  boolean isSwitchoverFinalized();

  List<Pod> getInitialInstances();

  List<Pod> getTotalInstances();

  List<Pod> getRestartedInstances();

  Map<Pod, RestartReasons> getPodRestartReasonsMap();

  @Value.Check
  default void check() {
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

  class Builder extends ImmutableClusterRestartState.Builder {
  }

  static Builder builder() {
    return new Builder();
  }

}
