/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDoneable;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;

@ApplicationScoped
public class PostgresBackupScheduler
    extends AbstractCustomResourceScheduler<StackGresBackupConfig,
      StackGresBackupConfigList, StackGresBackupConfigDoneable> {

  @Inject
  public PostgresBackupScheduler(KubernetesClientFactory clientFactory) {
    super(clientFactory,
        StackGresBackupConfigDefinition.CONTEXT,
        StackGresBackupConfig.class,
        StackGresBackupConfigList.class,
        StackGresBackupConfigDoneable.class);
  }

  public PostgresBackupScheduler() {
    super(null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
