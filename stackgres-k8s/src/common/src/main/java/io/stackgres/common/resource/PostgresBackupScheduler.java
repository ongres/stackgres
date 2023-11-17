/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostgresBackupScheduler
    extends AbstractCustomResourceScheduler<StackGresBackupConfig, StackGresBackupConfigList> {

  public PostgresBackupScheduler() {
    super(StackGresBackupConfig.class, StackGresBackupConfigList.class);
  }

}
