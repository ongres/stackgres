/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.CustomResourceComparisonDelegator;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;
import io.stackgres.operator.conciliation.comparator.ResourceComparator;

@ApplicationScoped
public class ShardedClusterResourceComparisonDelegator
    extends CustomResourceComparisonDelegator<StackGresShardedCluster> {

  private final Instance<ResourceComparator> comparators;

  private final ResourceComparator defaultComparator;

  @Inject
  public ShardedClusterResourceComparisonDelegator(
      @Any Instance<ResourceComparator> comparators,
      @ReconciliationScope(value = StackGresShardedCluster.class, kind = "HasMetadata")
          ResourceComparator defaultComparator) {
    this.comparators = comparators;
    this.defaultComparator = defaultComparator;
  }

  @Override
  protected ResourceComparator getComparator(HasMetadata r1) {
    Instance<ResourceComparator> instance = comparators
        .select(new ReconciliationScopeLiteral(StackGresShardedCluster.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultComparator;
    } else {
      return instance.get();
    }
  }

}
