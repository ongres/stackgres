/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupLabelMapper implements LabelMapperForBackup {

  @Override
  public String appName() {
    return StackGresContext.BACKUP_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresBackup resource) {
    return getKeyPrefix(resource) + StackGresContext.BACKUP_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresBackup resource) {
    return getKeyPrefix(resource) + StackGresContext.BACKUP_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresBackup resource) {
    return getKeyPrefix(resource) + StackGresContext.BACKUP_UID_KEY;
  }

}
