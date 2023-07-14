/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.AbstractComparator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ReconciliationScope(value = StackGresDbOps.class, kind = "HasMetadata")
public class DbOpsDefaultComparator extends AbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }
}
