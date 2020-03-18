/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;

@ApplicationScoped
public class PgPoolingConfigScheduler
    extends AbstractCustomResourceScheduler<StackGresPgbouncerConfig,
    StackGresPgbouncerConfigList, StackGresPgbouncerConfigDoneable> {

  public PgPoolingConfigScheduler() {
    super(
        StackGresPgbouncerConfigDefinition.NAME,
        StackGresPgbouncerConfig.class,
        StackGresPgbouncerConfigList.class,
        StackGresPgbouncerConfigDoneable.class);
  }

}
