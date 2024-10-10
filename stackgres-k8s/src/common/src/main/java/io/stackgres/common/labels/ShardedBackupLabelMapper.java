/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresKeys;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedBackupLabelMapper implements LabelMapperForShardedBackup {

  @Override
  public String appName() {
    return StackGresKeys.SHARDED_BACKUP_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresShardedBackup resource) {
    return getKeyPrefix(resource) + StackGresKeys.SHARDED_BACKUP_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresShardedBackup resource) {
    return getKeyPrefix(resource) + StackGresKeys.SHARDED_BACKUP_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresShardedBackup resource) {
    return getKeyPrefix(resource) + StackGresKeys.SHARDED_BACKUP_UID_KEY;
  }

}
