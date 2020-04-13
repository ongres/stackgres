/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Locale;

public enum BackupPhase {

  RUNNING,
  COMPLETED,
  FAILED;

  public String label() {
    return name().substring(0, 1).toUpperCase(Locale.US)
        + name().substring(1).toLowerCase(Locale.US);
  }

}
