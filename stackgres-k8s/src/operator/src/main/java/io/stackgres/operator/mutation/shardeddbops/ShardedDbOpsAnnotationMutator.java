/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class ShardedDbOpsAnnotationMutator
    extends AbstractAnnotationMutator<StackGresShardedDbOps, ShardedDbOpsReview>
    implements ShardedDbOpsMutator {

  private static final long LATEST = StackGresVersion.LATEST.getVersionAsNumber();

  @Override
  public Map<String, String> getAnnotationsToOverwrite(StackGresShardedDbOps resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (LATEST > version) {
      return Map.of(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    }
    return Map.of();
  }

}
