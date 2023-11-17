/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.stackgres.common.StackGresVersion;
import jakarta.enterprise.util.AnnotationLiteral;

public class OperatorVersionBinderLiteral extends AnnotationLiteral<OperatorVersionBinder>
    implements OperatorVersionBinder {

  private static final long serialVersionUID = 1L;

  @Override
  public StackGresVersion startAt() {
    return StackGresVersion.OLDEST;
  }

  @Override
  public StackGresVersion stopAt() {
    return StackGresVersion.LATEST;
  }

}
