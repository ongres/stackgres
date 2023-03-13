/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.shardedcluster;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.fixture.VersionedFixture;

public class ShardedClusterFixture extends VersionedFixture<StackGresShardedCluster> {

  public static final String POSTGRES_LATEST_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().get(0).get();

  public ShardedClusterFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_CLUSTER_DEFAULT_JSON);
    return this;
  }

  public ShardedClusterFixture empty() {
    fixture = new StackGresShardedCluster();
    return this;
  }

  public ShardedClusterFixture withSpec() {
    if (fixture.getSpec() == null) {
      fixture.setSpec(new StackGresShardedClusterSpec());
    }
    return this;
  }

  public ShardedClusterFixture withCoordinator() {
    if (fixture.getSpec().getCoordinator() == null) {
      fixture.getSpec().setCoordinator(new StackGresShardedClusterCoordinator());
    }
    return this;
  }

  public ShardedClusterFixture withCoordinatorPods() {
    withSpec();
    if (fixture.getSpec().getCoordinator().getPod() == null) {
      fixture.getSpec().getCoordinator().setPod(new StackGresClusterPod());
    }
    return this;
  }

  public ShardedClusterFixture withLatestPostgresVersion() {
    fixture.getSpec().getPostgres().setVersion(POSTGRES_LATEST_VERSION);
    return this;
  }

  public StackGresShardedClusterBuilder getBuilder() {
    return new StackGresShardedClusterBuilder(fixture);
  }

}
