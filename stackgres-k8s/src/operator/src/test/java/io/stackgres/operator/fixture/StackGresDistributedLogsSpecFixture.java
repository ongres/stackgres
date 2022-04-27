/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;

public class StackGresDistributedLogsSpecFixture {

  private StackGresDistributedLogsSpec spec;

  public StackGresDistributedLogsSpecFixture() {
    this.spec = new StackGresDistributedLogsSpec();
    this.spec.setPostgresServices(new StackGresDistributedLogsPostgresServices());
  }

  public StackGresDistributedLogsSpecFixture withPrimaryPostgresServices(
      StackGresPostgresService primary) {
    this.spec.getPostgresServices().setPrimary(primary);
    return this;
  }

  public StackGresDistributedLogsSpec build() {
    return this.spec;
  }

  public StackGresDistributedLogsSpecFixture withReplicasPostgresServices(
      StackGresPostgresService replicas) {
    this.spec.getPostgresServices().setReplicas(replicas);
    return this;
  }

}
