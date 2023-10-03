/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.shardedbackup;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class ShardedBackupFixture extends VersionedFixture<StackGresShardedBackup> {

  public ShardedBackupFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_BACKUP_DEFAULT_JSON);
    return this;
  }

  public StackGresShardedBackupBuilder getBuilder() {
    return new StackGresShardedBackupBuilder(fixture);
  }

}
