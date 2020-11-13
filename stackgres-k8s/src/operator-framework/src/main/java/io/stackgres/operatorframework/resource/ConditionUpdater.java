/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class ConditionUpdater<T, C extends Condition> {

  public void updateCondition(C condition, T context, KubernetesClient client) {
    Instant now = Instant.now();

    condition.setLastTransitionTime(now.toString());

    if (getConditions(context).stream()
        .filter(c -> c.getType().equals(condition.getType())
            && c.getStatus().equals(condition.getStatus()))
        .anyMatch(c -> Instant.parse(c.getLastTransitionTime())
            .isBefore(now.plus(1, ChronoUnit.MINUTES)))) {
      return;
    }

    // copy list of current conditions
    List<C> copyList =
        getConditions(context).stream()
            .filter(c -> !condition.getType().equals(c.getType()))
            .collect(Collectors.toList());

    copyList.add(condition);

    setConditions(context, copyList);

    patch(context, client);
  }

  protected abstract List<C> getConditions(T context);

  protected abstract void setConditions(T context, List<C> conditions);

  protected abstract void patch(T context, KubernetesClient client);

}
