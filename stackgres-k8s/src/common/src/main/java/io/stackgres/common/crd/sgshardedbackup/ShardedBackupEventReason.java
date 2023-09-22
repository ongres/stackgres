/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedbackup;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum ShardedBackupEventReason implements OperatorEventReason {

  BACKUP_CREATED(NORMAL, "ShardedBackupCreated"),
  BACKUP_UPDATED(NORMAL, "ShardedBackupUpdated"),
  BACKUP_DELETED(NORMAL, "ShardedBackupDeleted"),
  BACKUP_CONFIG_ERROR(WARNING, "ShardedBackupConfigFailed");

  private final Type type;
  private final String reason;

  ShardedBackupEventReason(Type type, String reason) {
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
