/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class BackupPipeline extends AbstractMutationPipeline<StackGresBackup, BackupReview> {

  @Inject
  public BackupPipeline(
      @Any Instance<BackupMutator> mutators) {
    super(mutators);
  }

}
