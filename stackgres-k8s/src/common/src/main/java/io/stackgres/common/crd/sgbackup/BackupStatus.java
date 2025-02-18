/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jooq.lambda.Seq;

public enum BackupStatus {

  PENDING,
  RUNNING,
  COMPLETED,
  FAILED;

  private static final List<String> FINISHED_STATUSES = List.of(COMPLETED.status, FAILED.status);

  private final String status;

  BackupStatus() {
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

  public static BackupStatus fromStatus(String status) {
    return Seq.of(values())
        .filter(backupPhase -> Objects.equals(status, backupPhase.status))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Backup status " + status + " is not any of "
                + Seq.of(values()).map(BackupStatus::status).toString(", ")));
  }

  public static boolean isCompleted(StackGresBackup backup) {
    return Optional.of(backup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .filter(COMPLETED.status::equals)
        .isPresent();
  }

  public static boolean isCompleted(Optional<StackGresBackup> backup) {
    return backup
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .filter(COMPLETED.status::equals)
        .isPresent();
  }

  public static boolean isFinished(StackGresBackup backup) {
    return Optional.of(backup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .filter(FINISHED_STATUSES::contains)
        .isPresent();
  }

}
