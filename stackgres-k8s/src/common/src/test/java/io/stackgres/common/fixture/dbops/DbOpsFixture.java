/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.dbops;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class DbOpsFixture extends VersionedFixture<StackGresDbOps> {

  public static final String POSTGRES_LATEST_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().get(0).get();

  public DbOpsFixture loadRestart() {
    fixture = readFromJson(STACKGRES_DB_OPS_RESTART_JSON);
    return this;
  }

  public DbOpsFixture loadMinorVersionUpgrade() {
    fixture = readFromJson(STACKGRES_DB_OPS_MINOR_VERSION_UPGRADE_JSON);
    return this;
  }

  public DbOpsFixture loadMajorVersionUpgrade() {
    fixture = readFromJson(STACKGRES_DB_OPS_MAJOR_VERSION_UPGRADE_JSON);
    return this;
  }

  public DbOpsFixture loadMajorVersionUpgradeWithLatestPostgresVersion() {
    fixture = readFromJson(STACKGRES_DB_OPS_MAJOR_VERSION_UPGRADE_JSON);
    fixture.getSpec().getMajorVersionUpgrade().setPostgresVersion(POSTGRES_LATEST_VERSION);
    return this;
  }

  public DbOpsFixture loadSecurityUpgrade() {
    fixture = readFromJson(STACKGRES_DB_OPS_SECURITY_UPGRADE_JSON);
    return this;
  }

  public DbOpsFixture loadPgbench() {
    fixture = readFromJson(STACKGRES_DB_OPS_PGBENCH_JSON);
    return this;
  }

  public DbOpsFixture loadSampling() {
    fixture = readFromJson(STACKGRES_DB_OPS_SAMPLING_JSON);
    return this;
  }

  public DbOpsFixture loadBenchmark() {
    fixture = readFromJson(STACKGRES_DB_OPS_BENCHMARK_JSON);
    return this;
  }

  public DbOpsFixture loadVacuum() {
    fixture = readFromJson(STACKGRES_DB_OPS_VACUUM_JSON);
    return this;
  }

  public DbOpsFixture loadRepack() {
    fixture = readFromJson(STACKGRES_DB_OPS_REPACK_JSON);
    return this;
  }

  public DbOpsSchedulingFixture scheduling() {
    return new DbOpsSchedulingFixture();
  }

  public StackGresDbOpsBuilder getBuilder() {
    return new StackGresDbOpsBuilder(fixture);
  }

}
