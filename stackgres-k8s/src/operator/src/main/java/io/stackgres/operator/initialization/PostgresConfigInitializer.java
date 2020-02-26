/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;

@ApplicationScoped
public class PostgresConfigInitializer
    extends AbstractDefaultCustomResourceInitializer<StackGresPostgresConfig> {

  @Inject
  public PostgresConfigInitializer(
      CustomResourceFinder<StackGresPostgresConfig> resourceFinder,
      CustomResourceScheduler<StackGresPostgresConfig> resourceScheduler,
      DefaultCustomResourceFactory<StackGresPostgresConfig> resourceFactory) {
    super(resourceFinder, resourceScheduler, resourceFactory);
  }

  public PostgresConfigInitializer() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
