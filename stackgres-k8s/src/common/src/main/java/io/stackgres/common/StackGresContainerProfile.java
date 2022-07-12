/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.math.BigDecimal;
import java.util.function.Function;

import io.fabric8.kubernetes.client.CustomResource;

public interface StackGresContainerProfile {

  StackGresKind getKind();

  String getName();

  Function<BigDecimal, BigDecimal> getCpuFormula();

  Function<BigDecimal, BigDecimal> getMemoryFormula();

  default boolean isContainerProfileFor(Class<? extends CustomResource<?, ?>> kind) {
    return getKind().getKindType().isAssignableFrom(kind);
  }

  default String getNameWithPrefix() {
    return getKind().getContainerPrefix() + getName();
  }

}
