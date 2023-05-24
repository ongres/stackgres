/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class BackupConfigPipeline
    extends AbstractMutationPipeline<StackGresBackupConfig, BackupConfigReview> {

  @Inject
  public BackupConfigPipeline(
      @Any Instance<BackupConfigMutator> mutators) {
    super(mutators);
  }

}
