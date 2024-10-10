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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Predicates;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresVersion;
import jakarta.enterprise.inject.Instance;

public abstract class AbstractDiscoverer<T>
    implements AnnotationFinder {

  private final Map<StackGresVersion, List<T>> hub =
      Arrays.stream(StackGresVersion.values())
      .collect(Collectors.toMap(Function.identity(), v -> new ArrayList<>()));

  private final Map<StackGresVersion, List<T>> registryHub =
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

  /**
   * Return the factories bound to the version of the operator that created the resource and to
   * whether its images are retrieved from the StackGres images registry (see
   * {@link OperatorVersionBinder#registry()}).
   */
  protected List<T> getFactories(StackGresVersion version, boolean registryEnabled) {
    return (registryEnabled ? registryHub : hub).get(version);
  }

  protected List<T> getFactories(GenerationContext<?> context) {
    return getFactories(context.getVersion(), context.isRegistryEnabled());
  }

  /**
   * Apply {@code action} to every list of factories (for each version and registry binding).
   */
  protected void forEachFactories(Consumer<List<T>> action) {
    hub.values().forEach(action);
    registryHub.values().forEach(action);
  }

  private void appendResourceFactory(T found) {
    OperatorVersionBinder operatorVersionTarget = getAnnotation(
        found, OperatorVersionBinder.class);
    final StackGresVersion startAt = Optional.of(operatorVersionTarget.from())
        .filter(Predicates.not(StackGresVersion.UNDEFINED::equals))
        .orElse(StackGresVersion.OLDEST);
    final StackGresVersion stopAt = Optional.of(operatorVersionTarget.to())
        .filter(Predicates.not(StackGresVersion.UNDEFINED::equals))
        .orElse(StackGresVersion.LATEST);

    final RegistryBinding registry = operatorVersionTarget.registry();

    for (int ordinal = startAt.ordinal();
         ordinal <= stopAt.ordinal(); ordinal++) {
      StackGresVersion version = StackGresVersion.values()[ordinal];
      if (registry != RegistryBinding.ENABLED) {
        hub.get(version).add(found);
      }
      if (registry != RegistryBinding.DISABLED) {
        registryHub.get(version).add(found);
      }
    }
  }

}
