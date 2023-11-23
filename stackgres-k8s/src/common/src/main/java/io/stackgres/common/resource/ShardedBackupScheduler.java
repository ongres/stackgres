/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedBackupScheduler
    extends AbstractCustomResourceScheduler<StackGresShardedBackup, StackGresShardedBackupList> {

  public ShardedBackupScheduler() {
    super(StackGresShardedBackup.class, StackGresShardedBackupList.class);
  }

}
