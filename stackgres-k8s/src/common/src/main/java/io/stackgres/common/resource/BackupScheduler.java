/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;

@ApplicationScoped
public class BackupScheduler
    extends AbstractCustomResourceScheduler<StackGresBackup, StackGresBackupList> {

  public BackupScheduler() {
    super(StackGresBackup.class, StackGresBackupList.class);
  }

}
