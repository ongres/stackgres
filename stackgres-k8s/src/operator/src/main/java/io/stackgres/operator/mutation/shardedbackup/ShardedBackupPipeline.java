/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedbackup;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedBackupPipeline
    extends AbstractMutationPipeline<StackGresShardedBackup, StackGresShardedBackupReview> {

  @Inject
  public ShardedBackupPipeline(
      @Any Instance<ShardedBackupMutator> mutators) {
    super(mutators);
  }

}
