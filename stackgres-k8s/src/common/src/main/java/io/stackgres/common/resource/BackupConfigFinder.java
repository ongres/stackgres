/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BackupConfigFinder
    extends AbstractCustomResourceFinder<StackGresBackupConfig> {

  /**
   * Create a {@code BackupConfigFinder} instance.
   */
  @Inject
  public BackupConfigFinder(KubernetesClient client) {
    super(client, StackGresBackupConfig.class, StackGresBackupConfigList.class);
  }

}
