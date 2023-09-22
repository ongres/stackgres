/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedbackup;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class ShardedBackupAnnotationMutator
    extends AbstractAnnotationMutator<StackGresShardedBackup, ShardedBackupReview>
    implements ShardedBackupMutator {

  // On version removed change this code to use the oldest one
  private static final long VERSION_1_4 = StackGresVersion.V_1_4.getVersionAsNumber();

  @Override
  public Map<String, String> getAnnotationsToOverwrite(StackGresShardedBackup resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (VERSION_1_4 > version) {
      return Map.of(StackGresContext.VERSION_KEY, StackGresVersion.V_1_4.getVersion());
    }
    return Map.of();
  }

}
