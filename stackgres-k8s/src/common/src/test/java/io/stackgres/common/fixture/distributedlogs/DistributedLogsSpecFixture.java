/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.distributedlogs;

import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.testutil.fixture.Fixture;

public class DistributedLogsSpecFixture extends Fixture<StackGresDistributedLogsSpec> {

  public DistributedLogsSpecFixture emptyPostgresServices() {
    if (fixture.getPostgresServices() == null) {
      fixture.setPostgresServices(new StackGresPostgresServices());
    }
    return this;
  }

  public DistributedLogsSpecFixture withPrimaryPostgresServices(
      StackGresPostgresService primary) {
    emptyPostgresServices();
    fixture.getPostgresServices().setPrimary(primary);
    return this;
  }

  public DistributedLogsSpecFixture withReplicasPostgresServices(
      StackGresPostgresService replicas) {
    emptyPostgresServices();
    fixture.getPostgresServices().setReplicas(replicas);
    return this;
  }

}
