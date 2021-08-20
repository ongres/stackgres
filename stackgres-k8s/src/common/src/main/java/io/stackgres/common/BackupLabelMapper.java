/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupLabelMapper implements LabelMapperForBackup {

  @Override
  public String appName() {
    return StackGresContext.BACKUP_APP_NAME;
  }

  @Override
  public String resourceNameKey() {
    return StackGresContext.BACKUP_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey() {
    return StackGresContext.BACKUP_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey() {
    return StackGresContext.BACKUP_UID_KEY;
  }

}
