/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.shardeddbops;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class ShardedDbOpsFixture extends VersionedFixture<StackGresShardedDbOps> {

  public ShardedDbOpsFixture loadRestart() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_RESTART_JSON);
    return this;
  }

  public ShardedDbOpsFixture loadMinorVersionUpgrade() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_MINOR_VERSION_UPGRADE_JSON);
    return this;
  }

  public ShardedDbOpsFixture loadMajorVersionUpgrade() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_MAJOR_VERSION_UPGRADE_JSON);
    return this;
  }

  public ShardedDbOpsFixture loadSecurityUpgrade() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_SECURITY_UPGRADE_JSON);
    return this;
  }

  public ShardedDbOpsFixture loadResharding() {
    fixture = readFromJson(STACKGRES_SHARDED_DB_OPS_RESHARDING_JSON);
    return this;
  }

  public ShardedDbOpsSchedulingFixture scheduling() {
    return new ShardedDbOpsSchedulingFixture();
  }

  public StackGresShardedDbOpsBuilder getBuilder() {
    return new StackGresShardedDbOpsBuilder(fixture);
  }

}
