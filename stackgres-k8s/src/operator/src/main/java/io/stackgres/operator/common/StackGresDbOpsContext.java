/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresDbOpsContext
    extends StackGresClusterContext {

  public abstract StackGresDbOps getCurrentDbOps();

}
