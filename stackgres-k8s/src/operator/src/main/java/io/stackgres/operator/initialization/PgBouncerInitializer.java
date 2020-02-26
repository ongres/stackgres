/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;

@ApplicationScoped
public class PgBouncerInitializer extends
    AbstractDefaultCustomResourceInitializer<StackGresPgbouncerConfig> {

  @Inject
  public PgBouncerInitializer(
      CustomResourceFinder<StackGresPgbouncerConfig> resourceFinder,
      CustomResourceScheduler<StackGresPgbouncerConfig> resourceScheduler,
      DefaultCustomResourceFactory<StackGresPgbouncerConfig> resourceFactory) {
    super(resourceFinder, resourceScheduler, resourceFactory);
  }

  public PgBouncerInitializer() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
