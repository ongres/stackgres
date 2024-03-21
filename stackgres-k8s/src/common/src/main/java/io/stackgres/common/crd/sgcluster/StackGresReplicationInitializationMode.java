/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresReplicationInitializationMode {

  FROM_PRIMARY("FromPrimary"),
  FROM_REPLICA("FromReplica"),
  FROM_EXISTING_BACKUP("FromExistingBackup"),
  FROM_NEWLY_CREATED_BACKUP("FromNewlyCreatedBackup");

  private final @NotNull String mode;

  StackGresReplicationInitializationMode(@NotNull String mode) {
    this.mode = mode;
  }

  public String mode() {
    return mode;
  }

  @Override
  public @NotNull String toString() {
    return mode;
  }

  public static StackGresReplicationInitializationMode fromString(String value) {
    for (StackGresReplicationInitializationMode mode : StackGresReplicationInitializationMode.values()) {
      if (mode.toString().equals(value)) {
        return mode;
      }
    }
    throw new IllegalArgumentException("Unknwon replication initialization mode " + value);
  }

}
