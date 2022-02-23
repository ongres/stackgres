/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;
import io.stackgres.operator.conciliation.StackGresResourceComparisonDelegator;
import io.stackgres.operator.conciliation.comparator.ResourceComparator;

@ApplicationScoped
public class ScriptResourceComparisonDelegator
    extends StackGresResourceComparisonDelegator<StackGresScript> {

  private final Instance<ResourceComparator> comparators;

  private final ResourceComparator defaultComparator;

  @Inject
  public ScriptResourceComparisonDelegator(
      @Any Instance<ResourceComparator> comparators,
      @ReconciliationScope(value = StackGresScript.class, kind = "HasMetadata")
          ResourceComparator defaultComparator) {
    this.comparators = comparators;
    this.defaultComparator = defaultComparator;
  }

  @Override
  protected ResourceComparator getComparator(HasMetadata r1) {
    Instance<ResourceComparator> instance = comparators
        .select(new ReconciliationScopeLiteral(StackGresScript.class, r1.getKind()));
    if (!instance.isResolvable()) {
      return defaultComparator;
    } else {
      return instance.get();
    }
  }

}
