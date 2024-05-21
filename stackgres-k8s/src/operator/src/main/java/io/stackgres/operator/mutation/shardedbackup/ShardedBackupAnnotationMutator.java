/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedbackup;

import java.util.Map;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedBackupAnnotationMutator
    extends AbstractAnnotationMutator<StackGresShardedBackup, StackGresShardedBackupReview>
    implements ShardedBackupMutator {

  private static final long LATEST = StackGresVersion.LATEST.getVersionAsNumber();

  @Override
  public Map<String, String> getAnnotationsToOverwrite(StackGresShardedBackup resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (LATEST > version) {
      return Map.of(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    }
    return Map.of();
  }

}
