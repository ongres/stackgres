/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedbackup;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jooq.lambda.Seq;

public enum ShardedBackupStatus {

  PENDING,
  RUNNING,
  COMPLETED,
  FAILED;

  private static final List<String> FINISHED_STATUSES = List.of(COMPLETED.status, FAILED.status);

  private final String status;

  ShardedBackupStatus() {
    this.status = name().substring(0, 1).toUpperCase(Locale.ROOT)
        + name().substring(1).toLowerCase(Locale.ROOT);
  }

  public String status() {
    return status;
  }

  @Override
  public String toString() {
    return status;
  }

  public static ShardedBackupStatus fromStatus(String status) {
    return Seq.of(values())
        .filter(backupPhase -> Objects.equals(status, backupPhase.status))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Backup status " + status + " is not any of "
                + Seq.of(values()).map(ShardedBackupStatus::status).toString(", ")));
  }

  public static boolean isFinished(StackGresShardedBackup backup) {
    return Optional.of(backup)
        .map(StackGresShardedBackup::getStatus)
        .map(StackGresShardedBackupStatus::getProcess)
        .map(StackGresShardedBackupProcess::getStatus)
        .filter(FINISHED_STATUSES::contains)
        .isPresent();
  }

  public static boolean isCompleted(StackGresShardedBackup backup) {
    return Optional.of(backup)
        .map(StackGresShardedBackup::getStatus)
        .map(StackGresShardedBackupStatus::getProcess)
        .map(StackGresShardedBackupProcess::getStatus)
        .filter(COMPLETED.status::equals)
        .isPresent();
  }

}
