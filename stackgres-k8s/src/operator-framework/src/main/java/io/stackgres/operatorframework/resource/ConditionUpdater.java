/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ConditionUpdater<T, C extends Condition> {

  public void updateCondition(C condition, T context) {
    Instant now = Instant.now();

    condition.setLastTransitionTime(now.toString());

    if (getConditions(context).stream()
        .anyMatch(c -> c.getType().equals(condition.getType())
            && c.getStatus().equals(condition.getStatus()))) {
      return;
    }

    // copy list of current conditions
    List<C> copyList =
        getConditions(context).stream()
            .filter(c -> !condition.getType().equals(c.getType()))
            .collect(Collectors.toList());

    copyList.addFirst(condition);

    setConditions(context, copyList);
  }

  protected abstract List<C> getConditions(T context);

  protected abstract void setConditions(T context, List<C> conditions);

}
