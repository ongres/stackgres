/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;

@ApplicationScoped
public class PgBouncerInitializer extends
    AbstractDefaultCustomResourceInitializer<StackGresPgbouncerConfig> {

  @Inject
  public PgBouncerInitializer(CustomResourceFinder<StackGresPgbouncerConfig> resourceFinder,
      CustomResourceScheduler<StackGresPgbouncerConfig> resourceScheduler,
      DefaultCustomResourceFactory<StackGresPgbouncerConfig> resourceFactory,
      InitializationQueue queue) {
    super(resourceFinder, resourceScheduler, resourceFactory, queue);
  }

  public PgBouncerInitializer() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  void onStart(@Observes StartupEvent ev) {
    super.initialize();
  }

}
