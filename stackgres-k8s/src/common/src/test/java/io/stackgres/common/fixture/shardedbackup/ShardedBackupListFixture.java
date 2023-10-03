/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.shardedbackup;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupList;
import io.stackgres.testutil.fixture.Fixture;

public class ShardedBackupListFixture extends Fixture<StackGresShardedBackupList> {

  public ShardedBackupListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SHARDED_BACKUP_LIST_JSON);
    return this;
  }

  public ShardedBackupListFixture withJustFirstElement() {
    if (fixture.getItems() != null && !fixture.getItems().isEmpty()) {
      fixture.setItems(fixture.getItems().subList(0, 1));
    }
    return this;
  }

}
