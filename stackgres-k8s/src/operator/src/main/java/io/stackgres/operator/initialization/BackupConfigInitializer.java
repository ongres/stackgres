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
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;

@ApplicationScoped
public class BackupConfigInitializer extends
    AbstractDefaultCustomResourceInitializer<StackGresBackupConfig> {

  @Inject
  public BackupConfigInitializer(CustomResourceFinder<StackGresBackupConfig> resourceFinder,
      CustomResourceScheduler<StackGresBackupConfig> resourceScheduler,
      DefaultCustomResourceFactory<StackGresBackupConfig> resourceFactory,
      InitializationQueue queue) {
    super(resourceFinder, resourceScheduler, resourceFactory, queue);
  }

  public BackupConfigInitializer() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  void onStart(@Observes StartupEvent ev) {
    super.initialize();
  }

}
