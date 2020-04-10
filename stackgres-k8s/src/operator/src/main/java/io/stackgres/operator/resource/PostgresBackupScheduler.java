/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDoneable;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;

@ApplicationScoped
public class PostgresBackupScheduler
    extends AbstractCustomResourceScheduler<StackGresBackupConfig,
      StackGresBackupConfigList, StackGresBackupConfigDoneable> {

  @Inject
  public PostgresBackupScheduler(KubernetesClientFactory clientFactory) {
    super(clientFactory,
        StackGresBackupConfigDefinition.NAME,
        StackGresBackupConfig.class,
        StackGresBackupConfigList.class,
        StackGresBackupConfigDoneable.class);
  }

  public PostgresBackupScheduler() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
