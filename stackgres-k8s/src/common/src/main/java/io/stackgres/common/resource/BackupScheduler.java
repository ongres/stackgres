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
import io.stackgres.common.crd.sgbackup.StackGresBackupDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackupDoneable;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;

@ApplicationScoped
public class BackupScheduler
    extends AbstractCustomResourceScheduler<StackGresBackup,
    StackGresBackupList, StackGresBackupDoneable> {

  @Inject
  public BackupScheduler(KubernetesClientFactory clientFactory) {
    super(clientFactory,
        StackGresBackupDefinition.CONTEXT,
        StackGresBackup.class,
        StackGresBackupList.class,
        StackGresBackupDoneable.class);
  }

  public BackupScheduler() {
    super(null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
