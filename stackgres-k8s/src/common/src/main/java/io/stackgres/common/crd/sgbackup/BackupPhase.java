/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Locale;

public enum BackupPhase {

  PENDING,
  RUNNING,
  COMPLETED,
  FAILED;

  public String label() {
    return toString();
  }

  @Override
  public String toString() {
    return name().substring(0, 1).toUpperCase(Locale.ROOT)
        + name().substring(1).toLowerCase(Locale.ROOT);
  }

}
