/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;

@ApplicationScoped
public class BackupConfigFinder
    extends AbstractCustomResourceFinder<StackGresBackupConfig> {

  /**
   * Create a {@code BackupConfigFinder} instance.
   */
  @Inject
  public BackupConfigFinder(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresBackupConfig.class, StackGresBackupConfigList.class);
  }

  public BackupConfigFinder() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
