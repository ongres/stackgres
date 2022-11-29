/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresBackup.class, kind = "HasMetadata")
@ApplicationScoped
public class BackupDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresBackup> {

  @Inject
  public BackupDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
