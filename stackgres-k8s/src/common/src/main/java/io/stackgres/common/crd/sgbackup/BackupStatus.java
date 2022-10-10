/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Locale;
import java.util.Objects;

import org.jooq.lambda.Seq;

public enum BackupStatus {

  PENDING,
  RUNNING,
  COMPLETED,
  FAILED;

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

}
