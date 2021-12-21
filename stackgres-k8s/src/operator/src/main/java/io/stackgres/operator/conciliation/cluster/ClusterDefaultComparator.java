/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.StackGresAbstractComparator;

@ApplicationScoped
@ReconciliationScope(value = StackGresCluster.class, kind = "HasMetadata")
public class ClusterDefaultComparator extends StackGresAbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
      new SimpleIgnorePatch("/metadata/managedFields",
          "add"),
      new SimpleIgnorePatch("/metadata/annotations/"
          + StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY,
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }
}
