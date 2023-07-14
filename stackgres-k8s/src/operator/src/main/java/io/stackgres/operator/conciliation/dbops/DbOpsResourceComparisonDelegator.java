/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.CustomResourceComparisonDelegator;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;
import io.stackgres.operator.conciliation.comparator.ResourceComparator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsResourceComparisonDelegator
    extends CustomResourceComparisonDelegator<StackGresDbOps> {

  private final Instance<ResourceComparator> comparators;

  private final ResourceComparator defaultComparator;

  @Inject
  public DbOpsResourceComparisonDelegator(
      @Any Instance<ResourceComparator> comparators,
      @ReconciliationScope(value = StackGresDbOps.class, kind = "HasMetadata")
          ResourceComparator defaultComparator) {
    this.comparators = comparators;
    this.defaultComparator = defaultComparator;
  }

  @Override
  protected ResourceComparator getComparator(HasMetadata r1) {
    Instance<ResourceComparator> instance = comparators
        .select(new ReconciliationScopeLiteral(StackGresDbOps.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultComparator;
    } else {
      return instance.get();
    }
  }

}
