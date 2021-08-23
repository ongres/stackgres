/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.CLUSTER_IP;

import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;

public class StackGresPostgresServiceFixture {

  private StackGresPostgresService stackGresPostgresService = new StackGresPostgresService();

  public StackGresPostgresServiceFixture validService() {
    isEnabled(true);
    withType(CLUSTER_IP);
    return this;
  }

  public StackGresPostgresServiceFixture withType(StackGresPostgresServiceType type) {
    stackGresPostgresService.setType(type.name());
    return this;
  }

  public StackGresPostgresServiceFixture isEnabled(boolean enabled) {
    stackGresPostgresService.setEnabled(enabled);
    return this;
  }

  public StackGresPostgresService build() {
    return this.stackGresPostgresService;
  }

}
