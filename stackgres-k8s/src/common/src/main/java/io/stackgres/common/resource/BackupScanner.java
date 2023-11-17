/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BackupScanner
    extends AbstractCustomResourceScanner<StackGresBackup, StackGresBackupList> {

  /**
   * Create a {@code BackupScanner} instance.
   */
  @Inject
  public BackupScanner(KubernetesClient client) {
    super(client, StackGresBackup.class, StackGresBackupList.class);
  }

}
