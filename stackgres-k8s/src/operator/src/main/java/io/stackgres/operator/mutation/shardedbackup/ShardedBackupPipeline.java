/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedbackup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class ShardedBackupPipeline
    extends AbstractMutationPipeline<StackGresShardedBackup, ShardedBackupReview> {

  @Inject
  public ShardedBackupPipeline(
      @Any Instance<ShardedBackupMutator> mutators) {
    super(mutators);
  }

}
