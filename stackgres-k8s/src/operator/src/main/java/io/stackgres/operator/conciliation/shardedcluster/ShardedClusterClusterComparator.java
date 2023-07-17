/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.regex.Pattern;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.AbstractComparator;
import jakarta.enterprise.context.ApplicationScoped;

@ReconciliationScope(value = StackGresShardedCluster.class, kind = "SGCluster")
@ApplicationScoped
public class ShardedClusterClusterComparator extends AbstractComparator {

  private static final IgnorePatch[] IGNORE_PATTERS = {
      new AnnotationsIgnorePatch(
          StackGresContext.VERSION_KEY),
      new SimpleIgnorePatch("/spec/managedSql",
          "add"),
      new SimpleIgnorePatch("/spec/postgres/flavor",
          "add"),
      new SimpleIgnorePatch("/spec/replication",
          "add"),
      new SimpleIgnorePatch("/spec/postgresServices",
          "add"),
      new SimpleIgnorePatch("/spec/postgresServices/primary/type",
          "add"),
      new SimpleIgnorePatch("/spec/postgresServices/replicas/type",
          "add"),
      new SimpleIgnorePatch("/spec/replication/role",
          "add"),
      new SimpleIgnorePatch("/status",
          "add"),
      new PatchPattern(Pattern
          .compile("/spec/managedSql/scripts/\\d+"),
          "add"),
      new PatchPattern(Pattern
          .compile("/spec/toInstallPostgresExtensions/\\d+/build"),
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATTERS;
  }

}
