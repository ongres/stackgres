/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_INTERFACE",
    justification = "Ignoring the error since OperatorEventReason is not used more than as enum")
public enum OperatorEventReason implements io.stackgres.common.crd.OperatorEventReason {

  OPERATOR_ERROR(WARNING, "OperatorError");

  private final Type type;
  private final String reason;

  OperatorEventReason(Type type, String reason) {
    this.type = type;
    this.reason = reason;
  }

  @Override
  public String reason() {
    return reason;
  }

  @Override
  public Type type() {
    return type;
  }

}
