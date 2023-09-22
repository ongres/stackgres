/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupList;

@ApplicationScoped
public class ShardedBackupFinder
    extends AbstractCustomResourceFinder<StackGresShardedBackup> {

  /**
   * Create a {@code ShardedBackupFinder} instance.
   */
  @Inject
  public ShardedBackupFinder(KubernetesClient client) {
    super(client, StackGresShardedBackup.class, StackGresShardedBackupList.class);
  }

}
