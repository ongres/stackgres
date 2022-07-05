/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.AbstractComparator;

@ReconciliationScope(value = StackGresCluster.class, kind = "SGScript")
@ApplicationScoped
public class ClusterScriptComparator extends AbstractComparator {

  private static final IgnorePatch[] IGNORE_PATTERS = {
      new AnnotationsIgnorePatch(
          StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY,
          StackGresContext.VERSION_KEY),
      new PatchPattern(Pattern
          .compile("/spec/scripts/\\d+/id"),
          "add"),
      new PatchPattern(Pattern
          .compile("/spec/scripts/\\d+/version"),
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATTERS;
  }

}
