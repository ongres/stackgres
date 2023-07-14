/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class BackupConfigPipeline
    extends AbstractMutationPipeline<StackGresBackupConfig, BackupConfigReview> {

  @Inject
  public BackupConfigPipeline(
      @Any Instance<BackupConfigMutator> mutators) {
    super(mutators);
  }

}
