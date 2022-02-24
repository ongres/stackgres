/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;

@ApplicationScoped
public class PostgresBackupScheduler
    extends AbstractCustomResourceScheduler<StackGresBackupConfig, StackGresBackupConfigList> {

  public PostgresBackupScheduler() {
    super(StackGresBackupConfig.class, StackGresBackupConfigList.class);
  }

}
