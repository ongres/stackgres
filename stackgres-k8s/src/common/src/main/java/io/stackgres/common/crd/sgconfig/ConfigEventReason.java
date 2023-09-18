/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum ConfigEventReason implements OperatorEventReason {

  CONFIG_CREATED(NORMAL, "ConfigCreated"),
  CONFIG_UPDATED(NORMAL, "ConfigUpdated"),
  CONFIG_DELETED(NORMAL, "ConfigDeleted"),
  CONFIG_ERROR(WARNING, "ConfigFailed");

  private final Type type;
  private final String reason;

  ConfigEventReason(Type type, String reason) {
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
