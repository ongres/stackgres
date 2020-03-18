/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigList;

@ApplicationScoped
public class PgConfigScheduler
    extends AbstractCustomResourceScheduler<StackGresPostgresConfig,
    StackGresPostgresConfigList, StackGresPostgresConfigDoneable> {

  public PgConfigScheduler() {
    super(StackGresPostgresConfigDefinition.NAME, StackGresPostgresConfig.class,
        StackGresPostgresConfigList.class, StackGresPostgresConfigDoneable.class);
  }

}
