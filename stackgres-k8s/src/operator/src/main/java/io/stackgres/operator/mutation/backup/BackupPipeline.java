/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class BackupPipeline extends AbstractMutationPipeline<StackGresBackup, BackupReview> {

  @Inject
  public BackupPipeline(
      @Any Instance<BackupMutator> mutators) {
    super(mutators);
  }

}
