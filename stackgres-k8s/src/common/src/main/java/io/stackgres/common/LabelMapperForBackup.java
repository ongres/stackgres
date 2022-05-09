/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.crd.sgbackup.StackGresBackup;

public interface LabelMapperForBackup
    extends LabelMapper<StackGresBackup> {

  default String backupKey(StackGresBackup resource) {
    return getKeyPrefix(resource) + StackGresContext.BACKUP_KEY;
  }

}
