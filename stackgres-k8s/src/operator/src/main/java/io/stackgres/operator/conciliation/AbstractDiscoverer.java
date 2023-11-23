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

import com.google.common.base.Predicates;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresVersion;
import jakarta.enterprise.inject.Instance;

public abstract class AbstractDiscoverer<T>
    implements AnnotationFinder {

  protected final Map<StackGresVersion, List<T>> hub =
      Arrays.stream(StackGresVersion.values())
      .collect(Collectors.toMap(Function.identity(), v -> new ArrayList<>()));

  @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
      justification = "safe overridable method")
  protected AbstractDiscoverer(Instance<T> instance) {
    instance.select(new OperatorVersionBinderLiteral())
        .stream()
        .filter(this::isSelected)
        .forEach(this::appendResourceFactory);
  }

  public AbstractDiscoverer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  protected boolean isSelected(T found) {
    return true;
  }

  private void appendResourceFactory(T found) {
    OperatorVersionBinder operatorVersionTarget = getAnnotation(
        found, OperatorVersionBinder.class);
    final StackGresVersion startAt = Optional.of(operatorVersionTarget.startAt())
        .filter(Predicates.not(StackGresVersion.UNDEFINED::equals))
        .orElse(StackGresVersion.OLDEST);
    final StackGresVersion stopAt = Optional.of(operatorVersionTarget.stopAt())
        .filter(Predicates.not(StackGresVersion.UNDEFINED::equals))
        .orElse(StackGresVersion.LATEST);

    for (int ordinal = startAt.ordinal();
         ordinal <= stopAt.ordinal(); ordinal++) {
      StackGresVersion version = StackGresVersion.values()[ordinal];
      hub.get(version).add(found);
    }
  }

}
