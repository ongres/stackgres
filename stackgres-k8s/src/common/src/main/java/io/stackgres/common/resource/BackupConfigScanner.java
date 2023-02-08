/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;

@ApplicationScoped
public class BackupConfigScanner
    extends AbstractCustomResourceScanner<StackGresBackupConfig, StackGresBackupConfigList> {

  /**
   * Create a {@code BackupConfigScanner} instance.
   */
  @Inject
  public BackupConfigScanner(KubernetesClient client) {
    super(client, StackGresBackupConfig.class, StackGresBackupConfigList.class);
  }

}
