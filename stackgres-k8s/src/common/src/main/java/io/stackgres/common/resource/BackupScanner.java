/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;

@ApplicationScoped
public class BackupScanner
    extends AbstractCustomResourceScanner<StackGresBackup, StackGresBackupList> {

  /**
   * Create a {@code BackupScanner} instance.
   */
  @Inject
  public BackupScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresBackup.class, StackGresBackupList.class);
  }

  public BackupScanner() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
