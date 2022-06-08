/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;

import com.google.common.base.Predicates;
import io.stackgres.common.StackGresVersion;

public abstract class ResourceDiscoverer<T> {

  protected Map<StackGresVersion, List<T>> resourceHub;

  public void init(Instance<T> instance) {
    resourceHub = Arrays.stream(StackGresVersion.values())
        .collect(Collectors.toMap(Function.identity(), v -> new ArrayList<>()));

    instance.select(new OperatorVersionBinderLiteral()).stream().forEach(f -> {
      OperatorVersionBinder operatorVersionTarget = f.getClass()
          .getAnnotation(OperatorVersionBinder.class);
      final StackGresVersion startAt = Optional.of(operatorVersionTarget.startAt())
          .filter(Predicates.not(StackGresVersion.UNDEFINED::equals))
          .orElse(StackGresVersion.OLDEST);
      final StackGresVersion stopAt = Optional.of(operatorVersionTarget.stopAt())
          .filter(Predicates.not(StackGresVersion.UNDEFINED::equals))
          .orElse(StackGresVersion.LATEST);

      for (int ordinal = startAt.ordinal();
           ordinal <= stopAt.ordinal(); ordinal++) {
        StackGresVersion version = StackGresVersion.values()[ordinal];
        resourceHub.get(version).add(f);
      }
    });
  }

}
