/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.math.BigDecimal;
import java.util.function.Function;

public interface StackGresContainerProfile extends StackGresScopedObject {

  Function<BigDecimal, BigDecimal> getCpuFormula();

  Function<BigDecimal, BigDecimal> getMemoryFormula();

}
