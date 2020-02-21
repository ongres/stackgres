/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDoneable;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;

@ApplicationScoped
public class BackupScanner
    extends AbstractCustomResourceScanner<StackGresBackup, StackGresBackupList,
    StackGresBackupDoneable> {

  /**
   * Create a {@code BackupScanner} instance.
   */
  @Inject
  public BackupScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresBackupDefinition.NAME,
        StackGresBackup.class, StackGresBackupList.class,
        StackGresBackupDoneable.class);
  }

  public BackupScanner() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
