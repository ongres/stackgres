/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;

@ApplicationScoped
public class PostgresConfigInitializer
    extends AbstractDefaultCustomResourceInitializer<StackGresPostgresConfig> {

  void onStart(@Observes StartupEvent ev) {

    super.initialize();
  }
}
