/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import io.stackgres.jobs.configuration.JobsProperty;
import io.stackgres.operatorframework.resource.EventReason;

public enum RestartEventsReason implements EventReason {
  ;

  private final Type type;
  private final String reason;

  RestartEventsReason(Type type, String reason) {
    this.type = type;
    this.reason = reason;
  }

  @Override
  public String component() {
    return JobsProperty.DATABASE_OPERATION_CR_NAME.getString();
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
