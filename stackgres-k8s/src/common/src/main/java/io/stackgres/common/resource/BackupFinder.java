/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;

@ApplicationScoped
public class BackupFinder
    extends AbstractCustomResourceFinder<StackGresBackup> {

  /**
   * Create a {@code BackupFinder} instance.
   */
  @Inject
  public BackupFinder(KubernetesClient client) {
    super(client, StackGresBackup.class, StackGresBackupList.class);
  }

}
