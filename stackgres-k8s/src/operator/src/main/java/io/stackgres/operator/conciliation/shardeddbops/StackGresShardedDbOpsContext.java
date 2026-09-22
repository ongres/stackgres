/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import java.util.Optional;

import io.stackgres.common.ShardedClusterContext;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresShardedDbOpsContext
    extends GenerationContext<StackGresShardedDbOps>, ShardedClusterContext {

  StackGresConfig getConfig();

  Optional<StackGresShardedCluster> getFoundShardedCluster();

  Optional<StackGresCluster> getFoundCoordinator();

  Optional<StackGresInstanceProfile> getFoundProfile();

  @Override
  @Value.Lazy
  default StackGresShardedCluster getShardedCluster() {
    return getFoundShardedCluster()
        .orElseThrow(() -> new IllegalStateException(
            "SGShardedDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " has no SGShardedCluster " + getSource().getSpec().getSgShardedCluster()
                + " in its context since the operation is already completed."
                + " Use getFoundShardedCluster() when the code path may run for a"
                + " completed operation"));
  }

  @Value.Lazy
  default StackGresInstanceProfile getProfile() {
    return getFoundProfile()
        .orElseThrow(() -> new IllegalStateException(
            "SGShardedDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGShardedCluster " + getSource().getSpec().getSgShardedCluster()
                + " has no SGInstanceProfile in its context: "
                + getFoundShardedCluster()
                    .map(StackGresShardedCluster::getSpec)
                    .map(StackGresShardedClusterSpec::getCoordinator)
                    .map(StackGresClusterSpec::getSgInstanceProfile)
                    .orElse("<unknown>")));
  }

  @Value.Lazy
  default StackGresCluster getCoordinatorCluster() {
    return getFoundCoordinator()
        .orElseThrow(() -> new IllegalStateException(
            "SGShardedDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGShardedCluster " + getSource().getSpec().getSgShardedCluster()
                + " has no coordinator SGCluster in its context: "
                + StackGresShardedClusterUtil.getCoordinatorClusterName(
                    getSource().getSpec().getSgShardedCluster())));
  }

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  public static class Builder extends ImmutableStackGresShardedDbOpsContext.Builder {
  }

  public static Builder builder() {
    return new Builder();
  }

}
