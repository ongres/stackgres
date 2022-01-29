/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;

@ApplicationScoped
public class PgConfigScheduler
    extends
    AbstractCustomResourceScheduler<StackGresPostgresConfig, StackGresPostgresConfigList> {

  public PgConfigScheduler() {
    super(StackGresPostgresConfig.class, StackGresPostgresConfigList.class);
  }

}
