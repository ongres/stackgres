/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDoneable;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;

@ApplicationScoped
public class BackupScheduler
    extends AbstractCustomResourceScheduler<StackGresBackup,
    StackGresBackupList, StackGresBackupDoneable> {

  public BackupScheduler() {
    super(StackGresBackupDefinition.NAME,
        StackGresBackup.class,
        StackGresBackupList.class,
        StackGresBackupDoneable.class);
  }

}
