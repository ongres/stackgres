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
import io.stackgres.common.crd.sgprofile.StackGresProfile;
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

  Optional<StackGresProfile> getFoundProfile();

  @Override
  @Value.Lazy
  default StackGresShardedCluster getShardedCluster() {
    return getFoundShardedCluster()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGShardedDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target non existent SGShardedCluster "
                + getSource().getSpec().getSgShardedCluster()));
  }

  @Value.Lazy
  default StackGresProfile getProfile() {
    return getFoundProfile()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGShardedDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGShardedCluster " + getSource().getSpec().getSgShardedCluster()
                + " with a non existent SGInstanceProfile "
                + getFoundShardedCluster()
                    .map(StackGresShardedCluster::getSpec)
                    .map(StackGresShardedClusterSpec::getCoordinator)
                    .map(StackGresClusterSpec::getSgInstanceProfile)
                    .orElse("<unknown>")));
  }

  @Value.Lazy
  default StackGresCluster getCoordinatorCluster() {
    return getFoundCoordinator()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGShardedDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGShardedCluster " + getSource().getSpec().getSgShardedCluster()
                + " with a non existent coordinator SGCluster "
                + StackGresShardedClusterUtil.getCoordinatorClusterName(
                    getSource().getSpec().getSgShardedCluster())));
  }

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

}
