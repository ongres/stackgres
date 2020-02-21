/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDoneable;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigList;

@ApplicationScoped
public class BackupConfigFinder
    extends AbstractCustomResourceFinder<StackGresBackupConfig> {

  /**
   * Create a {@code BackupConfigFinder} instance.
   */
  @Inject
  public BackupConfigFinder(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresBackupConfigDefinition.NAME,
        StackGresBackupConfig.class, StackGresBackupConfigList.class,
        StackGresBackupConfigDoneable.class);
  }

  public BackupConfigFinder() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
